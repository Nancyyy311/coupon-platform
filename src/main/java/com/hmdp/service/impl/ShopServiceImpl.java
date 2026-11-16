package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.cache.ShopChangedEvent;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.exception.ParamException;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.ParamValidator;
import com.hmdp.utils.SystemConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * 商户查询（高性能）。
 *
 * 相对原版的改动：
 *   1. queryByID 走二级缓存（L1 Caffeine 5s → L2 Redis 30min+抖动 → 互斥重建）
 *   2. 删掉逻辑过期（它会返回旧数据），删掉递归重试（会栈溢出）
 *   3. update 之后广播失效所有实例的 L1，不再只删 Redis
 *
 * 为什么不给商户缓存加布隆过滤器：
 *   商户 id 自增、数据量十万级，空值缓存已经把穿透挡住了。
 *   布隆需要额外维护"全量预热 + 新增同步"，十万级数据下收益接近零，
 *   属于典型的"为面试而加组件"。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    /**
     * Redis GEO 用的是 Web Mercator 投影，纬度可表示范围是 ±85.05112878，不是地理上的 ±90。
     * 传 89 这种"看起来合法的纬度"同样会被 Redis 拒绝，所以边界必须按 Redis 的口径写。
     */
    private static final double MAX_LATITUDE = 85.05112878;
    private static final double MAX_LONGITUDE = 180.0;

    private final StringRedisTemplate stringRedisTemplate;
    private final CacheClient cacheClient;
    /** 缓存失效改由事务提交后的事件驱动，见 ShopCacheEvictListener */
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Result queryByID(Long id) {
        Shop shop = cacheClient.queryWithMutex(
                CACHE_SHOP_KEY, id, Shop.class, this::getById,
                CACHE_SHOP_TTL, TimeUnit.MINUTES
        );
        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        return Result.ok(shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺不能为空");
        }
        // 1. 先查变更前的快照。
        //    两个用途，缺一不可：
        //    ① 判断 id 是否存在。updateById 返回 false 有两种含义（id 不存在 / 值全相同），
        //       只看返回值分不清，会把"改成功了"报成"店铺不存在"。
        //    ② 地理索引按分类分 key，换分类要"从旧 key 移走"，提交后再查库已经拿不到旧 typeId。
        Shop before = getById(id);
        if (before == null) {
            return Result.fail("店铺不存在！");
        }

        // 1.5 写入口的参数校验（只校验这次真的传了的字段）。
        //     不校验的话，超长、负数要到 MySQL 才报错，被兜成"服务器异常"——
        //     和上一轮读接口踩的是同一类坑：调用方的错被说成服务端的错。
        validateForWrite(shop, false);
        // 坐标单独校验：更新允许只传一个，必须拿库里的值补齐之后再判断这一对是否可用，
        // 否则"只改经度"会被误判成"纬度为空"。
        validateEffectivePoint(shop, before);

        // 2. 更新数据库。先删缓存再更库是经典错误顺序——并发读会把旧值重新写回缓存，
        //    而且这次写入的 TTL 还是完整的 30 分钟，脏数据会活很久。
        updateById(shop);

        // 3. 发事件，由 ShopCacheEvictListener 在**事务提交之后**删缓存 + 同步 GEO 索引。
        //    不能在这里直接删：事务此时还没提交，其他实例收到广播回源查库，读到的仍是旧值，
        //    再写回缓存就是半小时的脏读（这个坑上一版就踩了）。
        eventPublisher.publishEvent(new ShopChangedEvent(
                id, before.getTypeId(), before.getX(), before.getY()));

        return Result.ok();
    }

    /**
     * 新增店铺后要发事件，两件事都靠它：
     *
     * ① 清空值缓存。新 id 不可能有旧值，但**空值缓存**可能已经存在：
     *    如果有人提前查过这个 id（遍历、爬虫、脏链接），Redis 里会留下一个 "" 的标记，
     *    TTL 2 分钟。不清掉的话，新店铺在这 2 分钟里会被读成"店铺不存在"。
     *
     * ② 写地理索引。这是原项目缺的一环：GEO 里的数据是脚本一次性灌进去的，
     *    新增店铺从来不写 GEO，于是"附近商户"永远看不到新店——库里明明有，
     *    查询也走对了链路，就是不出现，而且不报任何错。
     */
    @Override
    public boolean save(Shop shop) {
        validateForWrite(shop, true);
        // 库里的 x/y 是 double unsigned，越界能写进去；但 GEO 只认 Web Mercator 范围，
        // 写进去之后这家店在"附近商户"里永远不出现，而且不报错。
        String pointError = pointWriteError(shop.getX(), shop.getY());
        if (pointError != null) {
            throw new ParamException(pointError);
        }
        boolean saved = super.save(shop);
        if (saved && shop.getId() != null) {
            eventPublisher.publishEvent(new ShopChangedEvent(shop.getId()));
        }
        return saved;
    }

    /**
     * 写入口校验。按 tb_shop 各列的实际约束来，不多不少：
     *   name varchar(128) / images varchar(1024) / address varchar(255) /
     *   area varchar(128) / open_hours varchar(32) / 三个计数列 unsigned。
     *
     * 为什么放 service 而不是 controller：
     *   save/update 还有别的调用方（后台脚本、定时任务、单测）。只拦 controller，
     *   等于只拦住了"我想到的那个入口"。
     *
     * @param creating true=新增，必填项必须齐；false=部分更新，只校验传了的字段
     */
    private static void validateForWrite(Shop shop, boolean creating) {
        if (shop == null) {
            throw new ParamException("店铺数据不能为空");
        }
        if (creating) {
            ParamValidator.requireText(shop.getName(), 128, "店铺名称");
            ParamValidator.requirePresent(shop.getTypeId(), "店铺分类");
            ParamValidator.requireText(shop.getImages(), 1024, "店铺图片");
            ParamValidator.requireText(shop.getAddress(), 255, "店铺地址");
            // 这三列 NOT NULL 且没有默认值。不补 0 的话 insert 会以
            // "Column 'sold' cannot be null" 失败，用户看到的又是一句"服务器异常"。
            if (shop.getSold() == null) {
                shop.setSold(0);
            }
            if (shop.getComments() == null) {
                shop.setComments(0);
            }
            if (shop.getScore() == null) {
                shop.setScore(0);
            }
        } else {
            ParamValidator.optionalText(shop.getName(), 128, "店铺名称");
            ParamValidator.optionalText(shop.getImages(), 1024, "店铺图片");
            ParamValidator.optionalText(shop.getAddress(), 255, "店铺地址");
        }
        ParamValidator.optionalText(shop.getArea(), 128, "商圈");
        ParamValidator.optionalText(shop.getOpenHours(), 32, "营业时间");
        ParamValidator.notNegative(shop.getAvgPrice(), "人均价格");
        ParamValidator.notNegative(shop.getSold(), "销量");
        ParamValidator.notNegative(shop.getComments(), "评价数");
        ParamValidator.range(shop.getScore(), 0, 255, "评分");
    }

    /** 更新时用"本次传的值 + 库里的值"拼出最终坐标再校验，避免只改一半被判成缺失 */
    private static void validateEffectivePoint(Shop shop, Shop before) {
        Double x = shop.getX() != null ? shop.getX() : before.getX();
        Double y = shop.getY() != null ? shop.getY() : before.getY();
        String pointError = pointWriteError(x, y);
        if (pointError != null) {
            throw new ParamException(pointError);
        }
    }

    /**
     * 写入前判断坐标能不能落库，返回 null 表示可以。
     *
     * 这里要分两种情况，原因很实际：
     *   ① 几何上不合法（越界、NaN、Infinity）—— Redis 会拒绝，写进去也查不出来；
     *   ② 几何上合法但存不进去 —— tb_shop 的 x/y 是 **double unsigned**，
     *      经度 -1（西半球）本身没问题，落到这一列却会报 Out of range。
     *      当前数据集只有杭州，等于事实上只支持东经/北纬。
     * 两种情况给不同文案：前者说明"数值超范围"，后者说明"不支持负值"，
     * 合在一起说"坐标不合法"会让调用方不知道该往哪改。
     */
    static String pointWriteError(Double x, Double y) {
        if (!isUsableGeoPoint(x, y)) {
            return "坐标不合法";
        }
        if (x < 0 || y < 0) {
            return "坐标不能为负数";
        }
        return null;
    }

    /**
     * 附近商户。
     *
     * 说明一个已知边界（被问到要主动讲，不要等对方挖）：
     *   这里用的是"取前 from+pageSize 条再内存截断"，不是真正的深分页。
     *   原因是 Redis 的 GEO 查询没有 offset 语义，只有 count。
     *   页数越深，单次拉取量越大，翻到第 100 页会明显变慢。
     *   数据量真正上来（几十万商户）时应该换 ES 的 geo_distance 查询，
     *   它支持 from/size。现在商户量级下，这个写法够用且实现成本最低。
     */
    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 页码必须先兜底。原因有两个，都是实测出来的：
        //   ① 这条链路有两种取数方式（GEO / 数据库分页），MyBatis-Plus 会把 current<=0
        //      钳成第 1 页，而下面的 GEO 分支是自己算下标的 —— from 会算出负数，
        //      list.get(-10) 抛 IndexOutOfBoundsException，匿名用户传 current=-1
        //      就能把接口打成"服务器异常"。
        //   ② 两种分支对同一个入参给出不同结果，本身就是不一致。
        // 所以统一在这里归一化，与 MyBatis-Plus 的分页语义保持一致。
        int pageNo = normalizePage(current);

        if (x == null || y == null) {
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(pageNo, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }
        // 坐标同理，而且更严重：越界、NaN、Infinity 会被 Redis 直接拒绝
        // （invalid longitude,latitude pair），异常一路上抛变成"服务器异常"。
        // 这是调用方传错了参数，不是服务端故障，必须在进入 Redis 之前拦掉。
        if (!isUsableGeoPoint(x, y)) {
            log.warn("[附近商户] 坐标不合法，已拒绝 typeId={} x={} y={}", typeId, x, y);
            return Result.fail("坐标不合法");
        }

        // 用 long 算下标，不能留在 int 上：
        //   current 是 int，× 每页条数在 int 里会溢出。比如 current=429496730、每页 5 条，
        //   乘积 2147483650 溢出成 -2147483646，`LIMIT 0 -2147483646` 会被 Redis
        //   直接拒绝（COUNT must be > 0），最后又变成一句"服务器异常"。
        //   换成 long 后乘积最大 1e10，远在 long 范围内，再怎么传都不会溢出。
        long from = (long) (pageNo - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        long end = (long) pageNo * SystemConstants.DEFAULT_PAGE_SIZE;

        String key = "shop:geo:" + typeId;
        Circle circle = new Circle(new Point(x, y), new Distance(5000));
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().radius(
                key,
                circle,
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeDistance()
                        .sortAscending()
                        .limit(end)
        );

        if (results == null) {
            return Result.ok(Collections.emptyList());
        }

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() <= from) {
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Double> distanceMap = new HashMap<>();
        // 从下标 from 开始遍历，而不是 list.stream().skip(from)：
        // skip 会先走一遍前 from 个元素再逐个丢弃，语义上是"过滤"；
        // 而这里要的是"从第 from 条开始取"，本来就是下标动作，for 直接表达，不多绕一层。
        // 上一行已经用 `list.size() <= from` 挡掉了越界，所以这里转 int 是安全的。
        for (int i = (int) from; i < list.size(); i++) {
            GeoResult<RedisGeoCommands.GeoLocation<String>> result = list.get(i);
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            distanceMap.put(shopIdStr, result.getDistance().getValue());
        }

        String idStr = StrUtil.join(",", ids);
        List<Shop> shopList = query()
                .in("id", ids)
                .last("ORDER BY FIELD(id," + idStr + ")")
                .list();

        for (Shop shop : shopList) {
            shop.setDistance(distanceMap.get(shop.getId().toString()));
        }

        return Result.ok(shopList);
    }

    /**
     * 页码归一化：null 或小于 1 一律按第 1 页。
     *
     * 抽成静态方法是为了能直接用单测覆盖边界（0 / 负数 / null），
     * 不必为了测一行判断去搭 Redis 和 MyBatis 的桩。
     */
    static int normalizePage(Integer current) {
        return current == null || current < 1 ? 1 : current;
    }

    /**
     * 坐标是否可用：既要排除越界，也要排除 NaN / Infinity。
     *
     * 为什么 NaN 单独列出来：`?x=NaN` 能被 Spring 正常绑定成 Double.NaN，
     * 而任何与 NaN 的比较都是 false，所以"范围判断"本身挡不住它 ——
     * 必须显式用 isNaN() 判，否则它会一路传到 Redis 再炸回来。
     */
    static boolean isUsableGeoPoint(Double x, Double y) {
        if (x == null || y == null) {
            return false;
        }
        if (x.isNaN() || y.isNaN() || x.isInfinite() || y.isInfinite()) {
            return false;
        }
        return x >= -MAX_LONGITUDE && x <= MAX_LONGITUDE
                && y >= -MAX_LATITUDE && y <= MAX_LATITUDE;
    }
}
