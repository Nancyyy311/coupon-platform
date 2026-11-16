package com.hmdp.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 秒杀链路的业务指标（可观测性）。
 *
 * 为什么必须有这一层：
 *   之前的"优化生效了"全靠压测报告里的 QPS/P99 说事。但压测只能证明"快"，
 *   证明不了"挡掉了多少"、"失败是哪种失败"、"回滚有没有真的发生"。
 *   线上出问题时，QPS 曲线是平的，而用户已经在投诉了——因为失败被业务码吞掉了。
 *
 *   加了指标之后，下面这些问题的答案是一条 Prometheus 查询，而不是"我觉得"：
 *     · 卖完之后，有多少请求在本地就被挡掉了？        -> local_block / request{result="sold_out"}
 *     · MQ 挂了，失败是超时、熔断还是路由失败？        -> mq_send_fail{reason=...}
 *     · 库存回补到底执行了没有，还是全被幂等守卫拦了？ -> stock_rollback{result=...}
 *     · 缓存 L1 命中了多少？（证明二级缓存不是白加）   -> cache_l1_hits / (hits+misses)
 *
 * 命名约定：
 *   hmdp. 前缀避免和框架自带指标（http.server.requests、jvm.*）混在一起；
 *   Prometheus 端点会把点号转成下划线，所以最终看到的是 hmdp_seckill_request_total。
 *
 * 关于标签爆炸（面试可能会问）：
 *   这里所有标签都是"有限枚举"（result 只有 9 种、reason 只有 4 种），
 *   绝不会把 userId / voucherId 放进标签——那会让时间序列数量随用户数增长，
 *   是 Prometheus 最常见的反模式。
 */
@Component
@RequiredArgsConstructor
public class SeckillMetrics {

    /** 秒杀请求结果：成功 */
    public static final String RESULT_SUCCESS = "success";
    /** 售罄（Lua 返回 1） */
    public static final String RESULT_SOLD_OUT = "sold_out";
    /** 一人一单（Lua 返回 2） */
    public static final String RESULT_DUPLICATE = "duplicate";
    /** 库存未预热（Lua 返回 3） */
    public static final String RESULT_NOT_READY = "not_ready";
    /** 活动未开始（Lua 返回 4） */
    public static final String RESULT_NOT_START = "not_start";
    /** 活动已结束（Lua 返回 5） */
    public static final String RESULT_ENDED = "ended";
    /** 被限流拦下（Redis 令牌桶，跨实例精确速率） */
    public static final String RESULT_RATE_LIMITED = "rate_limited";
    /** 被本地窗口拦下（零 Redis IO）。和上一条分开统计，才能看出本地粗筛挡了多少 */
    public static final String RESULT_RATE_LIMITED_LOCAL = "rate_limited_local";
    /** 发送 MQ 失败 / 脚本异常 */
    public static final String RESULT_ERROR = "error";

    private static final String SECKILL_REQUEST = "hmdp.seckill.request";
    private static final String SECKILL_LOCAL_BLOCK = "hmdp.seckill.local_block";
    private static final String MQ_SEND_FAIL = "hmdp.mq.send.fail";
    private static final String STOCK_ROLLBACK = "hmdp.stock.rollback";
    private static final String DLQ_HANDLED = "hmdp.dlq.handled";
    private static final String SECKILL_QUERY = "hmdp.seckill.query";

    private final MeterRegistry registry;

    /**
     * 指标对象本地缓存。
     * Micrometer 内部虽然有 registry 级缓存，但每次 builder().register() 仍会走一遍查找，
     * 秒杀接口是 3 万 QPS 的热路径，这点开销没必要重复付。
     */
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    /** 秒杀请求按结果计数：分母是全量请求，分子是各类失败，一眼能看出失败结构 */
    public void seckillResult(String result) {
        counter(SECKILL_REQUEST, "result", result).increment();
    }

    /** 本地售罄标识挡掉的请求数（这些请求一个 Redis 命令都没发） */
    public void localSoldOutBlock() {
        counter(SECKILL_LOCAL_BLOCK, "type", "sold_out").increment();
    }

    /**
     * MQ 发送失败。
     *
     * @param reason exception=同步抛异常 / breaker=熔断期快速失败 / nack=broker 未确认
     *               / returned=路由失败被退回
     */
    public void mqSendFail(String reason) {
        counter(MQ_SEND_FAIL, "reason", reason).increment();
    }

    /**
     * 库存回补。
     *
     * @param mode   full=还库存+释放占位 / stock=只还库存
     * @param result executed=真的回补了 / skipped=被幂等守卫拦下 / failed=执行异常
     */
    public void stockRollback(String mode, String result) {
        counter(STOCK_ROLLBACK, "mode", mode, "result", result).increment();
    }

    /** 死信处置次数（含"查库发现已落库、决定不回滚"的情况） */
    public void dlqHandled(String action) {
        counter(DLQ_HANDLED, "action", action).increment();
    }

    /**
     * 结果查询按状态计数。
     *
     * pending 的占比是这套异步架构最该盯的一条曲线：
     *   正常应该"查询一两次就转成 CREATED"，如果 pending 长期居高不下，
     *   说明消费者跟不上或者消息堆积了——这是 QPS 曲线看不出来的问题。
     *
     * @param state PENDING / CREATED / CLOSED / FAILED / forbidden
     */
    public void seckillResultQuery(String state) {
        counter(SECKILL_QUERY, "state", state).increment();
    }

    private Counter counter(String name, String... tags) {
        StringBuilder key = new StringBuilder(name);
        for (String tag : tags) {
            key.append('|').append(tag);
        }
        return counters.computeIfAbsent(key.toString(),
                k -> Counter.builder(name).tags(tags).register(registry));
    }
}
