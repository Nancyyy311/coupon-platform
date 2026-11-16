package com.hmdp.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 雪花 ID 的单元测试。
 *
 * 订单号一旦重复，就是主键冲突 + 用户投诉，所以这里只关心两件事：
 * 唯一、且并发下也唯一。趋势递增是加分项，顺手也测了。
 */
class SnowflakeIdWorkerTest {

    private SnowflakeIdWorker worker;

    @BeforeEach
    void setUp() {
        worker = newWorker(0L, 0L);
    }

    private SnowflakeIdWorker newWorker(long workerId, long datacenterId) {
        SnowflakeIdWorker w = new SnowflakeIdWorker();
        ReflectionTestUtils.setField(w, "workerId", workerId);
        ReflectionTestUtils.setField(w, "datacenterId", datacenterId);
        w.check();
        return w;
    }

    @Test
    @DisplayName("单线程连续发号 2 万个，不允许重复且必须单调递增")
    void uniqueAndIncreasing() {
        int count = 20_000;
        long previous = -1L;
        Set<Long> seen = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < count; i++) {
            long id = worker.nextId();
            assertTrue(seen.add(id), "出现重复 ID: " + id);
            assertTrue(id > previous, "ID 必须单调递增");
            previous = id;
        }
        assertEquals(count, seen.size());
    }

    @Test
    @DisplayName("8 线程并发各发 5000 个，跨线程不允许重复")
    void uniqueUnderConcurrency() throws InterruptedException {
        int threads = 8;
        int perThread = 5_000;
        Set<Long> all = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicates = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        if (!all.add(worker.nextId())) {
                            duplicates.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "并发发号超时");
        pool.shutdownNow();

        assertEquals(0, duplicates.get(), "并发下出现重复 ID");
        assertEquals(threads * perThread, all.size());
    }

    @Test
    @DisplayName("不同 workerId 生成的 ID 不冲突（多实例部署的前提）")
    void differentWorkersDoNotCollide() {
        SnowflakeIdWorker w1 = newWorker(1L, 0L);
        SnowflakeIdWorker w2 = newWorker(2L, 0L);

        Set<Long> ids = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < 2_000; i++) {
            // 交替发号，尽量落在同一毫秒内
            assertTrue(ids.add(w1.nextId()));
            assertTrue(ids.add(w2.nextId()));
        }
        assertEquals(4_000, ids.size());
    }

    @Test
    @DisplayName("workerId 越界必须在启动时就炸，不能等到运行时生成错号")
    void invalidWorkerIdRejected() {
        SnowflakeIdWorker bad = new SnowflakeIdWorker();
        ReflectionTestUtils.setField(bad, "workerId", 32L);
        ReflectionTestUtils.setField(bad, "datacenterId", 0L);

        assertThrows(IllegalArgumentException.class, bad::check);
    }

    @Test
    @DisplayName("反解时间：刚发的号，年龄必须是 0~1 秒，不能解出 2024 年")
    void timestampOfFreshId() {
        long before = System.currentTimeMillis();
        long id = worker.nextId();

        long ts = SnowflakeIdWorker.timestampOf(id);
        long age = SnowflakeIdWorker.ageMillis(id, System.currentTimeMillis());

        // 发号时间允许有毫秒级误差，但绝不允许差出一个量级——差一年就说明位段算错了
        assertTrue(Math.abs(ts - before) < 1_000L, "反解时间与发号时间偏差过大: " + (ts - before));
        assertTrue(age >= 0 && age < 1_000L, "刚生成的号年龄应为 0~1s，实际 " + age);
    }

    @Test
    @DisplayName("反解时间：把号往前拨 120 秒，年龄要跟着变成 120 秒（超时判定依赖这个）")
    void timestampOfAgedId() {
        long id = worker.nextId();
        long aged = id - (120_000L << 22);

        long age = SnowflakeIdWorker.ageMillis(aged, System.currentTimeMillis());

        assertTrue(age >= 120_000L && age < 121_000L, "拨回 120s 后年龄应约 120s，实际 " + age);
    }
}
