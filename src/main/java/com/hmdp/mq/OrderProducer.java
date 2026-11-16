package com.hmdp.mq;

import com.hmdp.monitor.SeckillMetrics;
import com.hmdp.utils.StockRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hmdp.config.RabbitMQConfig.ORDER_EXCHANGE;
import static com.hmdp.config.RabbitMQConfig.ORDER_ROUTING_KEY;

/**
 * 下单消息生产者（高可用）。
 *
 * 这里解决的是"Redis 扣了库存但消息没发出去"的幽灵扣减。三条失败路径，三条都要有人管：
 *   1) 发送时同步抛异常（连不上 broker）            -> 当场回滚 Redis，直接告诉用户失败
 *   2) broker 收到但 confirm 返回 nack             -> 异步回滚 Redis
 *   3) 消息到了交换机但没有队列接收（路由失败）      -> returns 回调里回滚
 *
 * 第 3 条是最容易漏的：
 *   yaml 里配了 publisher-returns: true，只代表"允许 broker 把消息退回来"，
 *   如果代码里没有 setReturnsCallback，退回的消息就只是被丢掉而已——
 *   而 broker 已经 ACK 了这次发布（confirm 是 ack），所以 confirm 回调也不会触发。
 *   结果是：库存扣了、消息没了、两边都没报错。日志里一行异常都没有。
 *   这类问题不会在正常拓扑下出现，但运维删队列、改 routing key、迁移时就会。
 *
 * 为什么不做本地消息表（outbox）：
 *   outbox 会给下单接口增加一次数据库写，而秒杀接口的全部价值就在于"不碰数据库"。
 *   缺口只在"Lua 成功 + 发送失败 + 进程当场崩溃"三件事同时发生时才出现，且可被对账发现。
 *   如果这是支付类资金场景，我会换 outbox —— 因为那里不允许"极小概率"。
 *
 * 关于 confirm 是异步的这件事（面试会问，要主动说）：
 *   nack 回来时用户早就收到 200 了。所以前端不能只看接口返回值就认定下单成功，
 *   必须凭订单号轮询查询。这也是为什么接口返回的是 orderId 而不是"下单成功"。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProducer {

    private final RabbitTemplate rabbitTemplate;
    private final StockRedisService stockRedisService;
    private final SeckillMetrics metrics;

    /**
     * 连续失败多少次后熔断。正常网络抖动不会连续失败这么多次；
     * 而 broker 真的挂了时，这个数字乘上单次连接超时（约 3s）就是"线程被占满"的时间。
     */
    @Value("${app.mq.breaker-failures:10}")
    private int breakerFailures;

    /** 熔断持续时长（毫秒）。设短一点：broker 恢复后能很快自动放流量试一下 */
    @Value("${app.mq.breaker-open-ms:3000}")
    private long breakerOpenMs;

    private final AtomicInteger consecutiveFailures = new AtomicInteger();

    /** 熔断截止时间戳；0 表示未熔断。用 volatile 是因为多个工作线程都要读 */
    private volatile long breakerOpenUntil = 0L;

    @PostConstruct
    public void init() {
        // 发布确认：publisher-confirm-type=correlated 时生效
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                return;
            }
            if (correlationData == null) {
                log.error("[消息未确认] 无关联数据，无法回滚，需人工核查 cause={}", cause);
                return;
            }
            // correlationId 编码了回滚所需的全部信息，避免在内存里维护 orderId -> 上下文 的映射
            String[] parts = correlationData.getId().split(":");
            Long orderId = Long.valueOf(parts[0]);
            Long voucherId = Long.valueOf(parts[1]);
            Long userId = Long.valueOf(parts[2]);
            log.error("[消息未确认] 回滚预扣库存 orderId={} cause={}", orderId, cause);
            metrics.mqSendFail("nack");
            stockRedisService.rollback(orderId, voucherId, userId);
        });

        // 路由失败：交换机收下了，但没有队列绑定到这个 routing key。
        // 这种情况 confirm 是 ack，所以只能靠 return 回调发现。
        // 注意：项目用的 spring-amqp 是 2.2.x（Boot 2.3 管理），只有 setReturnCallback；
        // 2.3+ 才改成 setReturnsCallback + ReturnedMessage，升级时这里要跟着改。
        rabbitTemplate.setReturnCallback(this::onReturned);
    }

    /**
     * broker 退回消息（mandatory 生效时）。
     *
     * 回调参数里没有 CorrelationData，所以从消息体反序列化出订单上下文。
     * 反序列化失败也必须记下来——那意味着有一条消息彻底丢了，只能人工对账。
     */
    private void onReturned(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        String body = message.getBody() == null
                ? "" : new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        log.error("[消息路由失败] replyCode={} replyText={} exchange={} routingKey={} body={}",
                replyCode, replyText, exchange, routingKey, body);
        metrics.mqSendFail("returned");
        try {
            Object converted = rabbitTemplate.getMessageConverter().fromMessage(message);
            if (converted instanceof OrderMessage) {
                OrderMessage order = (OrderMessage) converted;
                stockRedisService.rollback(order.getOrderId(), order.getVoucherId(), order.getUserId());
            } else {
                log.error("[消息路由失败] 无法识别消息类型，需人工回滚 body={}", body);
            }
        } catch (Exception e) {
            log.error("[消息路由失败] 反序列化失败，需人工回滚 body={}", body, e);
        }
    }

    /**
     * 发送下单消息。
     *
     * @return true 表示已交给 broker；false 表示发送失败且 Redis 已回滚
     */
    public boolean send(OrderMessage message) {
        // 熔断期：不再尝试连接 broker。
        // 这一步很关键——broker 挂掉时单次发送要等约 3 秒（连接超时）才失败，
        // 而秒杀接口每秒几百个请求，几秒钟就能把 Tomcat 的线程池占满，
        // 连带把不依赖 MQ 的接口一起拖死。熔断期直接快速失败，保住整个应用。
        // 注意：即使快速失败，Redis 预扣也已经发生过了，所以仍然必须回滚。
        if (System.currentTimeMillis() < breakerOpenUntil) {
            metrics.mqSendFail("breaker");
            stockRedisService.rollback(message.getOrderId(), message.getVoucherId(), message.getUserId());
            return false;
        }

        CorrelationData correlationData = new CorrelationData(
                message.getOrderId() + ":" + message.getVoucherId() + ":" + message.getUserId()
        );
        try {
            rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_ROUTING_KEY, message, correlationData);
            consecutiveFailures.set(0);
            return true;
        } catch (Exception e) {
            // broker 不可达：这一次请求必须当作失败处理，并把库存还回去
            log.error("[消息发送异常] orderId={}，回滚预扣库存", message.getOrderId(), e);
            metrics.mqSendFail("exception");
            stockRedisService.rollback(message.getOrderId(), message.getVoucherId(), message.getUserId());
            tripBreakerIfNeeded();
            return false;
        }
    }

    /** 熔断是否处于打开状态。给 Prometheus 的 gauge 用，0/1 比看日志快得多 */
    public boolean isBreakerOpen() {
        return System.currentTimeMillis() < breakerOpenUntil;
    }

    /** 连续失败计数，同样是给监控看的：它在涨但还没到阈值时，说明 broker 已经开始不稳定了 */
    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    /** 连续失败达到阈值就打开熔断，并在日志里留下明确的恢复时间点 */
    private void tripBreakerIfNeeded() {
        if (consecutiveFailures.incrementAndGet() < breakerFailures) {
            return;
        }
        consecutiveFailures.set(0);
        breakerOpenUntil = System.currentTimeMillis() + breakerOpenMs;
        log.error("[MQ 熔断] 连续发送失败 {} 次，暂停投递 {}ms。"
                        + "期间秒杀接口会快速失败（库存照常回滚），避免线程池被超时请求占满",
                breakerFailures, breakerOpenMs);
    }
}
