package com.hmdp.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 秒杀入口限流（两级：本地零 IO 粗筛 + Redis 令牌桶精筛）。
 *
 * 它要挡的不是"正常用户点太快"，而是"一个账号用脚本刷"：
 * 正常人的点击间隔是几百毫秒级，脚本可以一秒钟几千次。
 * 所以限流维度选"用户"而不是"IP"——IP 可以是代理池，账号要绑定手机号，成本高得多。
 *
 * ============================================================
 * 为什么必须有本地这一级（这是压测逼出来的结论，不是设计洁癖）：
 *   第一版只做了 Redis 令牌桶，压测结果很难看：
 *     限流关：QPS 3089，P99 44.8ms，10 秒内 Redis 执行 42,734 次业务脚本
 *     限流开：QPS 3029，P99 45.9ms，Redis 仍执行 78 + 40,889 次（业务 + 限流）
 *   原因很简单——**限流脚本本身也是一次 Redis 调用**。用一个 Redis 脚本去保护
 *   Redis，等于用一个瓶颈去保护同一个瓶颈：吞吐没变，只是把"业务判定"换成了
 *   "限流判定"。这类改动如果只看"有没有限流"这个功能，是发现不了的。
 *
 *   正确的分层是：本地先挡掉 99%，Redis 只处理漏过来的那 1%。
 *   本地判定的成本是纳秒级（一次哈希 + 自增），而且不产生任何网络 IO。
 *
 * 为什么本地不能单独用：
 *   本地计数是"每实例各一份"，N 个实例最多放过 N 倍流量；而且固定窗口在
 *   窗口边界会有 2 倍突刺。所以本地只做粗筛，阈值故意设成 2 倍，
 *   精确速率由 Redis 令牌桶保证。
 *
 * 一条重要的取舍：**限流器故障时放行（fail-open）**。
 *   限流是保护层，不是业务规则。Redis 抖动导致判断失败时，如果 fail-close，
 *   等于把一个可用的系统主动变成不可用——这比少挡几个脚本请求严重得多。
 *   代价是：限流器故障期间完全不设防，所以会打 WARN 日志，让告警能发现。
 */
@Slf4j
@Component
public class TokenBucketRateLimiter {

    private static final String KEY_PREFIX = "ratelimit:";

    private static final DefaultRedisScript<Long> SCRIPT;

    static {
        SCRIPT = new DefaultRedisScript<>();
        SCRIPT.setLocation(new ClassPathResource("ratelimit.lua"));
        SCRIPT.setResultType(Long.class);
    }

    /** 限流判定结果。区分本地拦截和 Redis 拦截，是为了让指标能说清"哪一层在挡流量" */
    public enum Decision {
        ALLOWED,
        /** 本地窗口拦下：零 Redis IO */
        LIMITED_LOCAL,
        /** Redis 令牌桶拦下：跨实例的精确速率 */
        LIMITED_REDIS
    }

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 本地固定窗口计数：key = 场景:用户:秒。
     * 用 Caffeine 而不是 ConcurrentHashMap，是因为它带容量上限和过期清理，
     * 不会因为用户数增长把内存吃满（这是本地缓存的经典事故）。
     */
    private final Cache<String, AtomicInteger> localWindow = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build();

    /** 一键开关：压测和对照实验要关掉它，否则测到的是限流阈值而不是系统容量 */
    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    /** 每秒补充多少令牌，即长期允许的每秒请求数 */
    @Value("${app.rate-limit.per-user-qps:5}")
    private int perUserQps;

    /** 桶容量，即允许的瞬时突发量 */
    @Value("${app.rate-limit.burst:5}")
    private int burst;

    /** 本地粗筛阈值（每实例每秒）。默认是精确值的 2 倍，抵消固定窗口的边界突刺 */
    @Value("${app.rate-limit.local-per-second:10}")
    private int localPerSecond;

    public TokenBucketRateLimiter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Decision acquire(String scene, Long userId) {
        if (!enabled) {
            return Decision.ALLOWED;
        }
        if (!passLocal(scene, userId)) {
            return Decision.LIMITED_LOCAL;
        }
        return passRedis(scene, userId) ? Decision.ALLOWED : Decision.LIMITED_REDIS;
    }

    /** 兼容旧调用：只关心"放不放行"的场景 */
    public boolean tryAcquire(String scene, Long userId) {
        return acquire(scene, userId) == Decision.ALLOWED;
    }

    private boolean passLocal(String scene, Long userId) {
        long second = System.currentTimeMillis() / 1000;
        String key = scene + ':' + userId + ':' + second;
        AtomicInteger counter = localWindow.get(key, k -> new AtomicInteger());
        return counter.incrementAndGet() <= localPerSecond;
    }

    private boolean passRedis(String scene, Long userId) {
        try {
            Long result = stringRedisTemplate.execute(
                    SCRIPT,
                    Collections.singletonList(KEY_PREFIX + scene + ":" + userId),
                    String.valueOf(perUserQps),
                    String.valueOf(burst),
                    "1"
            );
            return result == null || result == 1L;
        } catch (Exception e) {
            // fail-open：见类注释。这里必须打 WARN，否则"限流失效"会静默发生
            log.warn("[限流] 脚本执行失败，本次放行 scene={} userId={}", scene, userId, e);
            return true;
        }
    }

    // ============================================================
    // 纯函数版本：与 ratelimit.lua 使用同一套公式。
    // 抽出来是为了能用普通单测覆盖"补充/上限/拒绝"这些边界，
    // 而不必为了测十行算力去起一个内嵌 Redis。
    // ============================================================

    /**
     * 按经过的时间补充令牌，并封顶到桶容量。
     *
     * @param tokens   当前令牌数
     * @param lastMs   上次计算时间（毫秒）
     * @param nowMs    当前时间（毫秒）
     * @param rate     每秒补充速率
     * @param capacity 桶容量
     */
    public static double refill(double tokens, long lastMs, long nowMs, double rate, double capacity) {
        long deltaMs = nowMs - lastMs;
        if (deltaMs < 0) {
            // 时钟回退：按 0 处理，绝不反向扣令牌
            deltaMs = 0;
        }
        return Math.min(capacity, tokens + deltaMs / 1000.0 * rate);
    }

    /**
     * 尝试取走令牌。
     *
     * @return 剩余令牌；返回 -1 表示令牌不足，本次拒绝
     */
    public static double take(double tokens, double requested) {
        return tokens >= requested ? tokens - requested : -1;
    }
}
