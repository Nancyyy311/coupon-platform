package com.hmdp.utils;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 缓存互斥重建锁的单元测试。
 *
 * 这个类盯的是两个曾经真实的坑：
 *   1) 释放锁的时候无条件 delete。锁 TTL 到期后被别人持有，本线程的 finally 一删，
 *      第三个线程又能进来，互斥等于没有。修法是 value 存身份 + Lua 比对后删除。
 *   2) 没抢到锁就"等一次读不到直接返回 null"。上游会把这个 null 解释成
 *      "店铺不存在"，于是重建中的那一瞬间，用户看到的是查无此店。
 */
@ExtendWith(MockitoExtension.class)
class CacheClientLockTest {

    private static final String KEY = "cache:shop:1";
    private static final String LOCK_KEY = "lock:shop:1";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CacheClient cacheClient;
    private Shop shop;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheClient = new CacheClient(stringRedisTemplate);
        shop = new Shop();
        shop.setId(1L);
        shop.setName("测试店铺");
    }

    @Test
    @DisplayName("抢到锁：释放时走 Lua 身份校验，绝不直接 delete")
    void unlockWithCasInsteadOfDelete() {
        when(valueOperations.get(KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(1L);

        Shop result = cacheClient.queryWithMutex("cache:shop:", 1L, Shop.class,
                id -> shop, 30, TimeUnit.MINUTES);

        assertNotNull(result, "抢到锁的线程必须返回数据");
        assertEquals("测试店铺", result.getName());

        // 关键断言：释放锁走的是脚本（带身份校验），而不是裸 delete
        verify(stringRedisTemplate).execute(any(RedisScript.class), anyList(), any());
        verify(stringRedisTemplate, never()).delete(LOCK_KEY);
    }

    @Test
    @DisplayName("加锁时写入的是 UUID 身份，而不是固定的 1")
    void lockValueIsUniqueToken() {
        when(valueOperations.get(KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(1L);

        cacheClient.queryWithMutex("cache:shop:", 1L, Shop.class, id -> shop, 30, TimeUnit.MINUTES);

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).setIfAbsent(eq(LOCK_KEY), token.capture(), anyLong(), any(TimeUnit.class));
        // 固定值 "1" 会让"判断是不是自己的锁"永远成立，等于没校验
        assertEquals(36, token.getValue().length(), "锁身份应该是 UUID：" + token.getValue());
    }

    @Test
    @DisplayName("没抢到锁、重试也读不到缓存：降级查库，不能返回 null")
    void fallbackToDbInsteadOfNull() {
        // L2 一直是空，说明持锁线程还没把数据写回去
        when(valueOperations.get(KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        AtomicInteger dbCalls = new AtomicInteger();
        Shop result = cacheClient.queryWithMutex("cache:shop:", 1L, Shop.class, id -> {
            dbCalls.incrementAndGet();
            return shop;
        }, 30, TimeUnit.MINUTES);

        assertNotNull(result, "不能把'正在重建'返回成'不存在'");
        assertEquals(1, dbCalls.get(), "等锁超时后应该降级查一次库");
        // 没拿到锁就不该去释放锁，否则会把别人的锁删掉
        verify(stringRedisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    @DisplayName("没抢到锁、但持锁线程已重建完成：直接返回缓存，不再查库")
    void reuseBuiltCacheInsteadOfQueryingDb() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);
        // 第一次（L2）为空，重试时已经有值
        when(valueOperations.get(KEY)).thenReturn(null, JSONUtil.toJsonStr(shop));

        AtomicInteger dbCalls = new AtomicInteger();
        Shop result = cacheClient.queryWithMutex("cache:shop:", 1L, Shop.class, id -> {
            dbCalls.incrementAndGet();
            return shop;
        }, 30, TimeUnit.MINUTES);

        assertNotNull(result);
        assertEquals("测试店铺", result.getName());
        assertEquals(0, dbCalls.get(), "缓存已经有值就不该再打数据库");
    }

    @Test
    @DisplayName("空值缓存命中：直接判不存在，不去抢锁也不查库")
    void nullCacheShortCircuits() {
        when(valueOperations.get(KEY)).thenReturn("");

        AtomicInteger dbCalls = new AtomicInteger();
        Shop result = cacheClient.queryWithMutex("cache:shop:", 1L, Shop.class, id -> {
            dbCalls.incrementAndGet();
            return shop;
        }, 30, TimeUnit.MINUTES);

        assertEquals(null, result);
        assertEquals(0, dbCalls.get());
        verify(valueOperations, never())
                .setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("锁释放脚本执行异常：不影响本次查询结果")
    void unlockFailureDoesNotBreakQuery() {
        when(valueOperations.get(KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenThrow(new RuntimeException("redis down"));

        Shop result = cacheClient.queryWithMutex("cache:shop:", 1L, Shop.class,
                id -> shop, 30, TimeUnit.MINUTES);

        assertNotNull(result, "锁释放失败只该等 TTL，不该把查询结果也弄丢");
        assertEquals("测试店铺", result.getName());
    }
}
