package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * 二级缓存（高性能）。
 *
 * 相对原版 CacheClient 的改动：
 * ------------------------------------------------------------------
 * 1. 增加 L1（Caffeine）      原版只有 Redis，一次网络往返起步 0.3~1ms
 * 2. TTL 加随机抖动           原版固定 30 分钟，同一批 key 同时失效会一起打 DB
 * 3. 去掉逻辑过期             逻辑过期会返回旧数据，商户价格/营业状态不能这样
 * 4. 去掉递归重试             原版没抢到锁时递归调用自己，并发下会 StackOverflow
 * 5. 重建锁改为"带身份 + CAS 释放"  原版直接 delete，锁过期后会误删别人的锁
 * 6. 没抢到锁时不再返回 null    原版等一次读不到就当"数据不存在"，会把重建中说成查无此店
 * 7. 增加命中率统计           没有数据就无法证明 L1 是否真的有用
 *
 * 为什么不加布隆过滤器：
 *   商户表只有十万级数据，且 id 是自增的，空值缓存已经能挡住穿透。
 *   加布隆要多维护一个"预热 + 增量同步"的环节，收益几乎为零。
 *   判断依据：布隆值不值得上，看"被穿透的 key 数量 × 穿透频率"是否大到
 *   空值缓存撑不住，而不是看"这是不是一个高级组件"。
 *
 * L1 的一致性（原版方案里最容易漏掉的一题）：
 *   L1 在进程内，多实例各存一份。A 机器更新了商户，B 机器的 Caffeine 里还是旧值。
 *   解法是写操作后广播失效：见 ShopCacheInvalidator + CacheInvalidateListener。
 *   并且记住一条原则——**越是"错了会赔钱"的数据，越不该进 L1**。
 *   所以商户详情（可容忍秒级陈旧）进 L1，优惠券库存（不许错）不进。
 */
@Slf4j
@Component
public class CacheClient {

    /** L1 存活时间：短，因为一致性靠广播、兜底靠这个 TTL */
    private static final long LOCAL_TTL_SECONDS = 5;

    /** TTL 随机抖动上限，打散同一批 key 的失效时间 */
    private static final int TTL_JITTER_SECONDS = 300;

    /** 抖动按 TTL 的比例取（10%），避免短 TTL 被抖成几十倍 */
    private static final int TTL_JITTER_PERCENT = 10;

    /** 重建锁的持有时间：要覆盖"回源查库"的最长耗时；被误删才是真事故，所以给足余量 */
    private static final long REBUILD_LOCK_TTL_SECONDS = 10;

    /** 没抢到锁时最多等几次、每次多久（300ms 内还没建好就直接降级查库） */
    private static final int LOCK_WAIT_TIMES = 3;
    private static final long LOCK_WAIT_MILLIS = 100;

    /**
     * 释放锁：只有 value 还是我自己那次写的才删。
     *
     * 为什么必须是 Lua：判断和删除要原子。分开写（先 get 再 del）中间仍可能被别人抢到并写入新值，
     * 结果还是误删。这是"锁误删"的唯二正确解法之一（另一种是 Redisson 的看门狗 + 可重入计数）。
     */
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 一键开关：关掉后退化为"只有 Redis 的单级缓存"。
     * 用于 L1 出问题（比如某次改动忘了广播失效）时的紧急止损，改配置即可，不用发版。
     */
    @Value("${app.cache.l1-enabled:true}")
    private boolean l1Enabled;

    private final Cache<String, String> localCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(LOCAL_TTL_SECONDS, TimeUnit.SECONDS)
            .recordStats()
            .build();

    private final AtomicLong localHit = new AtomicLong();
    private final AtomicLong localMiss = new AtomicLong();
    private final AtomicLong redisHit = new AtomicLong();
    private final AtomicLong redisMiss = new AtomicLong();

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 查询：L1 → L2 → 互斥重建。
     */
    public <R, ID> R queryWithMutex(String keyPrefix, ID id, Class<R> type,
                                    Function<ID, R> dbFallback, long ttl, TimeUnit unit) {
        String key = keyPrefix + id;

        // 1. L1
        if (l1Enabled) {
            String localJson = localCache.getIfPresent(key);
            if (StrUtil.isNotBlank(localJson)) {
                localHit.incrementAndGet();
                return JSONUtil.toBean(localJson, type);
            }
            localMiss.incrementAndGet();
        }

        // 2. L2
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            redisHit.incrementAndGet();
            if (l1Enabled) {
                localCache.put(key, json);
            }
            return JSONUtil.toBean(json, type);
        }
        redisMiss.incrementAndGet();
        // 空值缓存命中：说明这个 id 确认不存在，直接返回
        if (json != null) {
            return null;
        }

        // 3. 互斥重建。只允许一个线程回源，其余线程短暂等待后重读缓存
        String lockKey = LOCK_SHOP_KEY + id;
        // 锁的身份：谁写的 by 谁删。没有它，锁过期之后本线程的 finally 会删掉别人的锁
        String lockToken = UUID.randomUUID().toString();
        boolean locked = tryLock(lockKey, lockToken);
        if (!locked) {
            // 原版在这里递归调用自己，高并发下会打穿栈；但"只等一次就返回 null"同样是错的：
            // 重建还没写完时返回 null，上游会把它当成"店铺不存在"直接报错。
            // 正确做法是短暂重试，仍拿不到就直接查库——宁可多查一次 DB，也不能把"正在重建"说成"不存在"。
            for (int i = 0; i < LOCK_WAIT_TIMES; i++) {
                sleepQuietly(LOCK_WAIT_MILLIS);
                String retryJson = stringRedisTemplate.opsForValue().get(key);
                if (StrUtil.isNotBlank(retryJson)) {
                    if (l1Enabled) {
                        localCache.put(key, retryJson);
                    }
                    return JSONUtil.toBean(retryJson, type);
                }
                // 空值缓存命中：确认不存在，不必再等
                if (retryJson != null) {
                    return null;
                }
            }
            log.debug("[缓存重建-降级查库] key={} 等锁超时，本次不走缓存", key);
            return dbFallback.apply(id);
        }

        try {
            // 双重检查：等锁期间可能已被其他线程重建完
            String doubleCheck = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(doubleCheck)) {
                if (l1Enabled) {
                    localCache.put(key, doubleCheck);
                }
                return JSONUtil.toBean(doubleCheck, type);
            }

            R fromDb = dbFallback.apply(id);
            if (fromDb == null) {
                // 空值缓存挡穿透：TTL 短，避免数据后续被创建了还一直判不存在
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            String valueJson = JSONUtil.toJsonStr(fromDb);
            // 抖动是基于"秒"算出来的，所以这里必须显式用 SECONDS，不能沿用入参的时间单位
            stringRedisTemplate.opsForValue()
                    .set(key, valueJson, withJitter(ttl, unit), TimeUnit.SECONDS);
            if (l1Enabled) {
                localCache.put(key, valueJson);
            }
            return fromDb;
        } finally {
            // 只删自己那把锁：如果本线程执行超过 TTL，锁已经过期被别人重新持有，
            // 此时无条件 delete 会把别人的锁删掉，第三个线程又能进来，互斥直接失效
            unlockQuietly(lockKey, lockToken);
        }
    }

    /** 写缓存（带随机抖动，统一按秒写入） */
    public void set(String key, Object value, long ttl, TimeUnit unit) {
        stringRedisTemplate.opsForValue()
                .set(key, JSONUtil.toJsonStr(value), withJitter(ttl, unit), TimeUnit.SECONDS);
    }

    /** 数据变更时清空本机 L1。跨实例的失效由广播负责 */
    public void invalidateLocal(String key) {
        localCache.invalidate(key);
    }

    /**
     * 分层命中率。这一行日志是"L1 到底有没有用"的唯一证据，
     * 压测报告里的缓存命中率必须来自这里，而不是形容词。
     */
    public String stats() {
        long lh = localHit.get(), lm = localMiss.get();
        long rh = redisHit.get(), rm = redisMiss.get();
        double l1Rate = (lh + lm) == 0 ? 0 : (double) lh / (lh + lm);
        double l2Rate = (rh + rm) == 0 ? 0 : (double) rh / (rh + rm);
        return String.format("L1 命中 %.2f%%（%d/%d） L2 命中 %.2f%%（%d/%d）",
                l1Rate * 100, lh, lh + lm, l2Rate * 100, rh, rh + rm);
    }

    // 下面四个只给监控用：Prometheus 里用 rate() 就能算出命中率曲线。
    // 不直接把"命中率"做成 gauge，因为比率在不同实例间不能直接相加，
    // 暴露原始计数让 PromQL 按实例聚合才是正确做法。
    public long getLocalHit() {
        return localHit.get();
    }

    public long getLocalMiss() {
        return localMiss.get();
    }

    public long getRedisHit() {
        return redisHit.get();
    }

    public long getRedisMiss() {
        return redisMiss.get();
    }

    /**
     * TTL 加抖动。
     *
     * 早先这里是无条件加 0~300 秒，对 30 分钟的商户缓存没问题（最多 +17%），
     * 但对短 TTL 是灾难：10 秒的 key 会被抖成 10~310 秒，最长翻了 30 倍。
     * 短 TTL 的意义就是"快速自愈"，被拉长之后，这个兜底就形同虚设。
     *
     * 所以改成按比例抖动（10%），并保留一个绝对上限：
     *   30 分钟 → +0~180 秒（仍然是 +10%，足够打散同一批 key）
     *   10 秒   → +0~1 秒（不再被放大）
     */
    private long withJitter(long ttl, TimeUnit unit) {
        long seconds = unit.toSeconds(ttl);
        if (seconds <= 0) {
            return seconds;
        }
        // 至少给 1 秒，否则极短的 TTL 等于没有抖动
        long bound = Math.max(1L, seconds * TTL_JITTER_PERCENT / 100);
        bound = Math.min(bound, TTL_JITTER_SECONDS);
        long jitter = ThreadLocalRandom.current().nextLong(bound + 1);
        return seconds + jitter;
    }

    private boolean tryLock(String key, String token) {
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, token, REBUILD_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok);
    }

    /** 释放锁：校验身份后删除，任一步失败都不抛给业务（锁有 TTL 兜底） */
    private void unlockQuietly(String key, String token) {
        try {
            Long deleted = stringRedisTemplate.execute(UNLOCK_SCRIPT,
                    Collections.singletonList(key), token);
            if (deleted != null && deleted == 0L) {
                log.debug("[缓存锁-已过期] key={} 锁已不属于当前线程，跳过删除", key);
            }
        } catch (Exception e) {
            log.warn("[缓存锁-释放失败] key={}，等待 TTL 自动过期", key, e);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
