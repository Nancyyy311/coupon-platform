package com.hmdp.cache;

import com.hmdp.entity.Shop;
import com.hmdp.utils.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 商户地理索引的写路径（GEO 同步）。
 *
 * 补的是原项目一个只靠"读"发现不了的洞：
 *   "附近商户"读的是 Redis GEO（shop:geo:{typeId}），但**没有任何代码往这个索引里写**。
 *   课程里的数据是 SQL 脚本 + 测试类一次性 ZADD 进去的，跑起来看上去一切正常。
 *   一旦真的走管理端：
 *     - 新增一家店（POST /shop）→ 库里有了，附近列表永远查不到它
 *     - 改坐标（PUT /shop）    → 库里搬走了，用户看到的还是旧位置和旧距离
 *     - 改分类（PUT /shop）    → 旧分类的"附近"里留着它，新分类里没有它
 *   三个都是"数据在库里是对的、在索引里是错的"，而且不报错、不留日志，只能靠人肉发现。
 *
 * 为什么按分类分 key，而不是一个 key 存全部 + 过滤：
 *   课程原设计就是 shop:geo:{typeId}。一次半径查询只扫一个分类的 ZSET，
 *   比"全量 GEO 再按 typeId 过滤"少一次全表扫描。代价是分类变更要搬 key——正是本类处理的。
 *
 * 为什么用 GEOADD 而不是先删后加：
 *   GEOADD 对同一 member 是"更新坐标"，天然幂等，不需要额外的原子性保证。
 *   只有跨分类才需要显式 ZREM，因为那是两个不同的 key，不做就会在旧 key 里留幽灵成员。
 *
 * 为什么放在事务提交之后调用（见 ShopCacheEvictListener）：
 *   事务没提交就写索引，一旦回滚，索引里就多了一个库里不存在的店——
 *   附近列表会查出 id 然后 IN 查询回空，前端表现为"少一条"，比报错更难查。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopGeoIndexer {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 把一次变更同步到 GEO 索引。
     *
     * @param event 变更事件，带变更前的 typeId / x / y
     * @param after 提交后的最新数据；null 表示店铺已被删除
     */
    public void sync(ShopChangedEvent event, Shop after) {
        Long shopId = event.getShopId();
        Long oldTypeId = event.getOldTypeId();

        if (after == null) {
            // 店铺没了：把旧分类里的成员清掉，否则附近列表会查出空 id
            if (oldTypeId != null) {
                remove(oldTypeId, shopId);
            }
            return;
        }

        boolean typeChanged = oldTypeId != null && !oldTypeId.equals(after.getTypeId());
        if (typeChanged) {
            remove(oldTypeId, shopId);
        }

        if (after.getTypeId() == null || after.getX() == null || after.getY() == null) {
            // 坐标或分类缺失的店铺进不了地理索引。不抛异常：事务已经提交，抛了也回不去，
            // 只会让管理端看到"保存失败"的假象；留下日志等运营补齐数据。
            log.warn("[GEO-跳过] shopId={} 缺少 typeId/坐标，无法写入地理索引", shopId);
            return;
        }

        boolean moved = typeChanged
                || !Objects.equals(event.getOldX(), after.getX())
                || !Objects.equals(event.getOldY(), after.getY());
        if (!moved) {
            // 只改了名字/图片/营业时间这类不影响位置的字段
            return;
        }

        stringRedisTemplate.opsForGeo().add(
                RedisConstants.SHOP_GEO_KEY + after.getTypeId(),
                new Point(after.getX(), after.getY()),
                String.valueOf(shopId)
        );
        log.info("[GEO-更新] shopId={} typeId={} x={} y={}{}",
                shopId, after.getTypeId(), after.getX(), after.getY(),
                typeChanged ? "（分类 " + oldTypeId + " -> " + after.getTypeId() + "）" : "");
    }

    private void remove(Long typeId, Long shopId) {
        stringRedisTemplate.opsForGeo().remove(
                RedisConstants.SHOP_GEO_KEY + typeId,
                String.valueOf(shopId)
        );
        log.info("[GEO-移除] shopId={} 已从分类 {} 的地理索引移除", shopId, typeId);
    }
}
