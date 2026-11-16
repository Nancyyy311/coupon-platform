package com.hmdp.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    /**
     * 验证码发送节流：同号 60 秒一次。SET NX 占位，占不到就是"刚发过"。
     */
    public static final String LOGIN_CODE_THROTTLE_KEY = "login:code:limit:";
    /**
     * 同号 24 小时发送总量。只靠 60 秒间隔挡不住"按秒发一年"的刷量，
     * 必须再压一个总量上限，否则接口就是一个匿名可达的免费短信发送器。
     */
    public static final String LOGIN_CODE_DAILY_KEY = "login:code:daily:";
    /**
     * 验证码输错次数。到上限直接烧掉验证码，把"穷举 100 万种"压成"最多试 5 次"。
     */
    public static final String LOGIN_CODE_FAIL_KEY = "login:code:fail:";
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    /**
     * 活动时间窗口，hash 结构，两个 field：begin / end，值为 epoch 毫秒。
     * 与库存一起预热，让步 seckill.lua 用 redis.call('time') 直接判断，
     * 下单路径不需要为了"活动是否开始"回查数据库。
     */
    public static final String SECKILL_TIME_KEY = "seckill:time:";
    /**
     * 预扣回滚标记，值为订单号本身，由 rollback.lua 用 SET NX EX 写入，TTL 24 小时。
     * 它承担两件事：
     *   ① 幂等守卫：同一笔订单重复回滚只生效一次（MQ 至少一次投递）
     *   ② 结果查询的判据：数据库查不到订单、但这个键存在 = 这笔单确实被回滚了，
     *      而不是"还在处理中"。前缀必须与 rollback.lua 里的 guardKey 保持一致。
     */
    public static final String SECKILL_ROLLBACK_KEY = "seckill:rollback:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feed:";
    /**
     * 关注关系投影：Set，member 是"我关注的人"的 userId。
     * 只用于求共同关注（SINTER），权威数据在 tb_follow。
     * 注意它是投影而不是真相：丢了必须能从库里重建，不能拿空集合当"没有共同关注"。
     */
    public static final String FOLLOWS_KEY = "follows:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";
}
