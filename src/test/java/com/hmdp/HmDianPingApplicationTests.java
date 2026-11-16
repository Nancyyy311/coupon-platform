package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

/**
 * 原版教程留下的联调脚本（需要 Redis / MySQL / MQ 都起着）。
 *
 * <p><b>为什么整类 {@code @Disabled}</b>：这三个方法不是测试，是课程里的"手动联调入口"——
 * 没有一条断言，全部靠 {@code System.out.println} 人眼看；而且都有副作用：
 * <ul>
 *   <li>{@code testHyperLogLog()} 往 Redis 写 {@code hl2}，实测留下 99 万个 member 且 TTL=-1（永不过期）</li>
 *   <li>{@code loadShopData()} 会把 {@code shop:geo:*} 全部重建</li>
 *   <li>{@code testSaveShop()} 覆盖 {@code cache:shop:1}</li>
 * </ul>
 * 更关键的是它标着 {@code @SpringBootTest}——只要它在，{@code mvn test} 就必须有 Redis/MySQL/MQ，
 * 换台干净机器直接红，等于把"能不能跑测试"绑死在本地环境上。
 *
 * <p>禁用后单测套件是 75 个、零外部依赖，任何机器 clone 下来 {@code mvn test} 直接跑。
 * 这三个方法保留原样，需要手工联调时去掉 {@code @Disabled} 即可。
 *
 * <p>另：原来的 {@code testIdWorker} 测的是 RedisIdWorker（INCR 自增 ID），
 * 改造后订单号换成雪花算法，该类变成死代码，已连测试一起删除。
 */
@Disabled("课程联调脚本：无断言、有副作用、依赖 Redis/MySQL/MQ，不作为单测执行")
@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private CacheClient cacheClient;

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testSaveShop() {
        Shop shop = shopService.getById(1L);
        // 改造后不再有"逻辑过期"，缓存统一是纯对象 JSON + TTL
        cacheClient.set(CACHE_SHOP_KEY + 1L, shop, 10L, TimeUnit.SECONDS);
    }

    @Test
    void loadShopData() {
        // 1.查询店铺信息
        List<Shop> list = shopService.list();
        // 2.把店铺分组，按照typeId分组，typeId一致的放到一个集合
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        // 3.分批完成写入Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            // 3.1.获取类型id
            Long typeId = entry.getKey();
            String key = "shop:geo:" + typeId;
            // 3.2.获取同类型的店铺的集合
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            // 3.3.写入redis GEOADD key 经度 纬度 member
            for (Shop shop : value) {
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(), new Point(shop.getX(), shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }

    @Test
    void testHyperLogLog() {
        String[] values = new String[1000];
        int j = 0;
        for (int i = 0; i < 1000000; i++) {
            j = i % 1000;
            values[j] = "user_" + i;
            if (j == 999) {
                // 发送到Redis
                stringRedisTemplate.opsForHyperLogLog().add("hl2", values);
            }
        }
        // 统计数量
        Long count = stringRedisTemplate.opsForHyperLogLog().size("hl2");
        System.out.println("count = " + count);
    }
}
