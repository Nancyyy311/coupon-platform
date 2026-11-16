package com.hmdp.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 定时任务互斥锁的单元测试。
 *
 * 这个类的返回值有三种语义，弄错一种就会导致"两个实例同时跑"或者"谁都不跑"：
 *   非空 token → 我拿到了，正常执行
 *   null       → 别人在跑，本次跳过
 *   空串       → Redis 挂了，降级执行（不能跳过，否则不需要 Redis 的归档任务也停了）
 *
 * 第 3 种最容易被简化的冲动干掉——"Redis 挂了就跳过"看起来更安全，
 * 但归档任务只操作 MySQL，Redis 挂了它照样能干活。
 */
@ExtendWith(MockitoExtension.class)
class JobLockTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private JobLock jobLock;

    @Test
    @DisplayName("锁空闲时拿到 token，token 非空（空串是降级标记，不能混用）")
    void acquireReturnsTokenWhenFree() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        String token = jobLock.tryAcquire("order-archive", 60);

        assertNotNull(token);
        assertFalse(token.isEmpty(), "拿到锁必须返回非空 token，空串是降级标记");
    }

    @Test
    @DisplayName("锁被别的实例占着时返回 null，调用方据此跳过本次执行")
    void acquireReturnsNullWhenBusy() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        assertNull(jobLock.tryAcquire("order-archive", 60));
    }

    @Test
    @DisplayName("Redis 不可用时降级为空串，不抛异常——归档任务不依赖 Redis，不能被锁拖停")
    void acquireDegradesWhenRedisDown() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenThrow(new RedisConnectionFailureException("connection refused"));

        String token = jobLock.tryAcquire("order-archive", 60);

        assertEquals("", token, "Redis 不可用应返回降级标记，而不是 null（null 会让任务跳过）");
    }

    @Test
    @DisplayName("锁 key 带 lock:job: 前缀并拼任务名，不同任务互不影响")
    void lockKeyIsNamespaced() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        jobLock.tryAcquire("stock-reconcile", 300);

        verify(valueOperations).setIfAbsent(
                eq("lock:job:stock-reconcile"), anyString(), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("没拿到锁（null）时释放是空操作，不能去删别人的锁")
    void releaseSkipsWhenTokenIsNull() {
        jobLock.release("order-archive", null);

        verify(stringRedisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    @DisplayName("降级（空串）时释放同样跳过：那次根本没加锁，删了就是误删")
    void releaseSkipsWhenDegraded() {
        jobLock.release("order-archive", "");

        verify(stringRedisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    @DisplayName("正常持有时用 Lua 带 token 释放，走 CAS 而不是裸 DEL")
    void releaseUsesLuaCas() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(1L);

        jobLock.release("order-archive", "token-abc");

        verify(stringRedisTemplate).execute(
                any(RedisScript.class), eq(Collections.singletonList("lock:job:order-archive")), eq("token-abc"));
    }

    @Test
    @DisplayName("释放时 Redis 异常不往外抛：锁有 TTL 兜底，不能因为解锁失败把任务结果改成失败")
    void releaseSwallowsRedisError() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenThrow(new RedisConnectionFailureException("down"));

        jobLock.release("order-archive", "token-abc");
        // 不抛异常即通过；锁会在 TTL 后自动过期
    }
}
