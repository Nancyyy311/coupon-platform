package com.hmdp.config;

import com.hmdp.mq.OrderProducer;
import com.hmdp.utils.CacheClient;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 把"已经数在内存里的值"暴露给 Prometheus（可观测性）。
 *
 * 指标分两类，做法不同：
 *   Counter（只增不减）：由业务代码在事件发生时 +1，见 SeckillMetrics。
 *   Gauge（可增可减的瞬时值）：这里把已有的 AtomicLong / 布尔状态挂上去，
 *     不额外维护第二份计数——两份计数迟早会不一致，这是监控里最常见的坑。
 *
 * 为什么命中率不直接做成一个 0~1 的 gauge：
 *   比率在多实例之间不能相加也不能取平均（每个实例的流量权重不同），
 *   正确做法是暴露分子分母，让 PromQL 用
 *   sum(rate(hmdp_cache_hits_total[1m])) / sum(rate(hmdp_cache_l1_requests_total[1m])) 算。
 */
@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final MeterRegistry meterRegistry;
    private final CacheClient cacheClient;
    private final OrderProducer orderProducer;

    @PostConstruct
    public void bindGauges() {
        // 缓存分层命中计数。pair 起来看才知道 L1 到底省掉了多少 Redis 往返
        Gauge.builder("hmdp.cache.access", cacheClient, CacheClient::getLocalHit)
                .description("缓存访问计数")
                .tag("level", "l1").tag("result", "hit")
                .register(meterRegistry);
        Gauge.builder("hmdp.cache.access", cacheClient, CacheClient::getLocalMiss)
                .description("缓存访问计数")
                .tag("level", "l1").tag("result", "miss")
                .register(meterRegistry);
        Gauge.builder("hmdp.cache.access", cacheClient, CacheClient::getRedisHit)
                .description("缓存访问计数")
                .tag("level", "l2").tag("result", "hit")
                .register(meterRegistry);
        Gauge.builder("hmdp.cache.access", cacheClient, CacheClient::getRedisMiss)
                .description("缓存访问计数")
                .tag("level", "l2").tag("result", "miss")
                .register(meterRegistry);

        // MQ 熔断状态：0 关闭 / 1 打开。熔断打开时这个值会持续 3 秒，
        // 配合 hmdp_mq_send_fail_total 就能区分"偶发失败"和"broker 真挂了"
        Gauge.builder("hmdp.mq.breaker.open", orderProducer,
                        p -> p.isBreakerOpen() ? 1 : 0)
                .description("MQ 发送熔断是否打开")
                .register(meterRegistry);

        // 连续失败数：它在涨但还没到阈值时，是 broker 开始不稳的最早信号
        Gauge.builder("hmdp.mq.send.fail.streak", orderProducer, OrderProducer::getConsecutiveFailures)
                .description("MQ 发送连续失败次数（未达熔断阈值前的早期信号）")
                .register(meterRegistry);
    }
}
