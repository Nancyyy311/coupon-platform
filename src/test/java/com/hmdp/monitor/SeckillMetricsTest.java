package com.hmdp.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 指标本身也要有测试，理由很实际：
 *   指标写错不会报错，只会让面板安静地显示 0。等到真出故障时，
 *   "监控没有报警"和"监控不存在"是完全一样的后果。
 *   所以我至少要钉住两件事：计数在累加、标签打得对（标签错了 PromQL 就查不出来）。
 */
class SeckillMetricsTest {

    private SimpleMeterRegistry registry;
    private SeckillMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new SeckillMetrics(registry);
    }

    @Test
    @DisplayName("同一结果累加，不同结果分别计数")
    void countsByResultTag() {
        metrics.seckillResult(SeckillMetrics.RESULT_SUCCESS);
        metrics.seckillResult(SeckillMetrics.RESULT_SUCCESS);
        metrics.seckillResult(SeckillMetrics.RESULT_SOLD_OUT);

        assertEquals(2.0, counter("hmdp.seckill.request", "result", "success").count(), 1e-9);
        assertEquals(1.0, counter("hmdp.seckill.request", "result", "sold_out").count(), 1e-9);
    }

    @Test
    @DisplayName("回滚指标两个标签维度都要能查到（mode + result）")
    void rollbackHasTwoDimensions() {
        metrics.stockRollback("full", "executed");
        metrics.stockRollback("full", "skipped");
        metrics.stockRollback("stock", "executed");

        assertEquals(1.0, counter("hmdp.stock.rollback", "mode", "full", "result", "executed").count(), 1e-9);
        assertEquals(1.0, counter("hmdp.stock.rollback", "mode", "full", "result", "skipped").count(), 1e-9);
        assertEquals(1.0, counter("hmdp.stock.rollback", "mode", "stock", "result", "executed").count(), 1e-9);
    }

    @Test
    @DisplayName("本地售罄拦截单独计数，用于证明本地拦截真的挡住了流量")
    void localBlockIsCountedSeparately() {
        metrics.localSoldOutBlock();
        metrics.localSoldOutBlock();
        assertEquals(2.0, counter("hmdp.seckill.local_block", "type", "sold_out").count(), 1e-9);
    }

    @Test
    @DisplayName("MQ 失败按原因分类，能区分超时/熔断/路由失败")
    void mqFailuresAreClassified() {
        metrics.mqSendFail("exception");
        metrics.mqSendFail("breaker");
        metrics.mqSendFail("breaker");
        assertEquals(1.0, counter("hmdp.mq.send.fail", "reason", "exception").count(), 1e-9);
        assertEquals(2.0, counter("hmdp.mq.send.fail", "reason", "breaker").count(), 1e-9);
    }

    private Counter counter(String name, String... tags) {
        return registry.get(name).tags(tags).counter();
    }
}
