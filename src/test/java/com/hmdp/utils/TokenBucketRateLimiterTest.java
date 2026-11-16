package com.hmdp.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 令牌桶的数学部分（与 ratelimit.lua 共用同一套公式）。
 *
 * 为什么测这个而不是直接测 Lua：
 *   Lua 的边界恰好是"补充、封顶、拒绝"这三件事，它们都是纯算术。
 *   用纯函数把算术钉死，Lua 侧只需要保证"和 Java 用同一个公式"，
 *   这比为了测十行算术去起一个内嵌 Redis 划算得多。
 */
class TokenBucketRateLimiterTest {

    private static final double RATE = 5.0;
    private static final double CAPACITY = 5.0;

    @Test
    @DisplayName("空桶经过足够时间后补满，且不会超过容量")
    void refillCapsAtCapacity() {
        // 空了 10 秒，按 5/s 能补 50 个，但容量只有 5
        assertEquals(CAPACITY, TokenBucketRateLimiter.refill(0, 0, 10_000, RATE, CAPACITY), 1e-9);
    }

    @Test
    @DisplayName("补充按毫秒精度计算，不是按整秒")
    void refillIsMillisecondPrecise() {
        // 200ms × 5/s = 1 个令牌
        assertEquals(3.0, TokenBucketRateLimiter.refill(2, 0, 200, RATE, CAPACITY), 1e-9);
    }

    @Test
    @DisplayName("时钟回退时按 0 处理，绝不反向扣令牌")
    void refillIgnoresClockGoingBackwards() {
        // nowMs < lastMs（主从切换后可能读到更早的时间）
        assertEquals(3.0, TokenBucketRateLimiter.refill(3, 10_000, 5_000, RATE, CAPACITY), 1e-9);
    }

    @Test
    @DisplayName("令牌足够时正常扣减")
    void takeSucceedsWhenEnoughTokens() {
        assertEquals(4.0, TokenBucketRateLimiter.take(5, 1), 1e-9);
    }

    @Test
    @DisplayName("令牌不足时返回 -1（约定值），而不是扣成负数")
    void takeFailsWhenNotEnoughTokens() {
        assertEquals(-1, TokenBucketRateLimiter.take(0.4, 1), 1e-9);
    }

    @Test
    @DisplayName("同一毫秒内连打 6 次：前 5 次放行，第 6 次拒绝")
    void burstIsBoundedByCapacity() {
        double tokens = CAPACITY;
        int allowed = 0;
        for (int i = 0; i < 6; i++) {
            double left = TokenBucketRateLimiter.take(tokens, 1);
            if (left < 0) {
                break;
            }
            tokens = left;
            allowed++;
        }
        assertEquals(5, allowed, "桶容量是 5，同一时刻最多只应放行 5 次");
        assertEquals(0.0, tokens, 1e-9);
    }

    @Test
    @DisplayName("稳态：桶空后每秒只能再放行 rate 个")
    void steadyStateMatchesConfiguredRate() {
        // 模拟第 0 秒打空一个桶，然后每秒尝试 10 次，持续 1 秒
        double tokens = 0;
        long now = 0;
        int allowedInOneSecond = 0;
        for (int i = 0; i < 10; i++) {
            now += 100; // 每 100ms 尝试一次
            tokens = TokenBucketRateLimiter.refill(tokens, now - 100, now, RATE, CAPACITY);
            double left = TokenBucketRateLimiter.take(tokens, 1);
            if (left >= 0) {
                tokens = left;
                allowedInOneSecond++;
            }
        }
        // 5/s 的速率，1 秒内最多放过 5 个（受容量上限与补充速度共同约束）
        assertTrue(allowedInOneSecond <= 5 && allowedInOneSecond >= 4,
                "1 秒内应放行 4~5 次，实际 " + allowedInOneSecond);
    }
}
