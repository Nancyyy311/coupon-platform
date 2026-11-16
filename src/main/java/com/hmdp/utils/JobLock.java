package com.hmdp.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务互斥锁（多实例下"只让一个节点跑"）。
 *
 * 为什么需要它：
 *   三个定时任务（关单 60s / 对账 5min / 归档每天 3:00）都是 {@code @Scheduled}，
 *   两个实例就会各跑一遍。幂等的任务重复跑只是浪费，不幂等的会出事——
 *   归档任务两个实例同时 INSERT 同一批订单，后者必然主键冲突，
 *   原实现直接 break 中止，表现为"每天归档只成功一个节点、另一个报 ERROR"。
 *
 * 为什么不是引入 XXL-Job / ShedLock：
 *   这个项目只有一个调度场景，为它加一个中间件（XXL-Job 还要单独部署调度中心 + 建库）
 *   或者加一个依赖（ShedLock 需要一个 LockProvider + 建表），成本远大于收益。
 *   用已有的 Redis 做互斥，二十行代码，行为可解释。真正的调度中心适合
 *   "任务多、需要人工触发、需要执行历史和失败重跑记录"的场景，不是这里。
 *
 * 锁的三个必要条件（面试会追问）：
 *   1. 加锁原子：SET key value NX EX ttl，一条命令，不能 GET 再 SET；
 *   2. 释放原子：Lua 里比较 value 再删，否则"判断完、删之前锁过期并被别人抢到"会误删别人的锁；
 *   3. TTL 兜底：进程被 kill 时锁不会永久残留，最坏等 TTL 过期后恢复调度。
 *
 * TTL 怎么定：取"正常耗时的十倍左右"，不是随便写。
 *   - 太短：任务没跑完锁就过期，第二个实例进来，互斥形同虚设；
 *   - 太长：进程崩溃后要等很久才能恢复，不过这期间任务本来就没人跑，代价可接受。
 *   真正的兜底不是 TTL，是任务本身的幂等——锁是"省资源 + 防报错"，幂等才是正确性。
 *
 * Redis 不可用时（fail-open）：锁只是优化，不是正确性保障，所以这里不阻断任务，
 *   打一条 warn 后照常执行，靠任务自身的幂等兜住。反过来 fail-closed 会让
 *   "Redis 挂了 → 归档也不跑了"，而归档根本不依赖 Redis。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobLock {

    private static final String KEY_PREFIX = "lock:job:";

    /** 降级标记：Redis 不可用时返回它，调用方照常执行，释放时跳过 */
    private static final String DEGRADED = "";

    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取任务锁。
     *
     * @param job        任务名（同一任务的所有实例必须用同一个名字）
     * @param ttlSeconds 锁自动过期时间，必须大于任务正常耗时
     * @return 拿到锁返回 token（非空，释放时用）；被别人占着返回 null（本次应跳过）；
     *         Redis 不可用返回空串（降级执行，不要跳过）
     */
    public String tryAcquire(String job, long ttlSeconds) {
        String key = KEY_PREFIX + job;
        String token = UUID.randomUUID().toString();
        try {
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, token, ttlSeconds, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(ok)) {
                // 打一条 debug：验证"两个实例只有一个在跑"时，光有"跳过"的日志不够，
                // 还得能对上"谁拿到了"，否则只能靠推断
                log.debug("[任务锁] {} 获取成功", job);
                return token;
            }
            log.debug("[任务锁] {} 已被其他实例持有，本次跳过", job);
            return null;
        } catch (Exception e) {
            // fail-open：锁是优化不是正确性保障，Redis 不可用时靠任务幂等兜底
            log.warn("[任务锁-降级] {} Redis 不可用，本次不加锁直接执行（依赖任务幂等）", job, e);
            return DEGRADED;
        }
    }

    /** 释放锁。没拿到锁（null 或降级标记）时什么都不做，避免误删别人的锁 */
    public void release(String job, String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        try {
            Long deleted = stringRedisTemplate.execute(UNLOCK_SCRIPT,
                    Collections.singletonList(KEY_PREFIX + job), token);
            if (deleted != null && deleted == 0L) {
                log.debug("[任务锁-已过期] {} 的锁已不属于当前实例，跳过删除", job);
            }
        } catch (Exception e) {
            log.warn("[任务锁-释放失败] {}，等待 TTL 自动过期", job, e);
        }
    }
}
