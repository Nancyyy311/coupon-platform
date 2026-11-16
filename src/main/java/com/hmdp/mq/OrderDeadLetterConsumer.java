package com.hmdp.mq;

import com.hmdp.monitor.SeckillMetrics;
import com.hmdp.service.VoucherOrderTxService;
import com.hmdp.utils.StockRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.hmdp.config.RabbitMQConfig.ORDER_DLQ;

/**
 * 死信处置（高可用）。
 *
 * 死信队列如果只有"收尸"没有"处置"，它本质上就是一个垃圾桶：
 * 消息躺在里面，Redis 那份库存永远回不来，用户还以为自己抢到了。
 * 真正需要的是"进死信 = 这笔单确定没成 = 把预扣还回去"。
 *
 * 处置前必须查一次数据库，因为进死信有两种成因：
 *   ① 真的没落库（数据库一直失败、重试用尽）      -> 回滚 Redis
 *   ② 其实落库成功了，只是 ACK 没送到 broker      -> 绝对不能回滚，否则超卖
 * 这两种在消息体上完全一样，只有查库能区分。死信是低频路径，多一次查询完全值得。
 *
 * 为什么这里把异常全部吞掉：
 *   DLQ 没有配置下级死信（也没必要配），容器又是 default-requeue-rejected=false，
 *   一旦抛异常，消息会被直接丢弃——那才是真正的"消息丢了"。
 *   宁可把错误留在日志里等人工处理，也不要让它从队列里消失。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.dlq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderDeadLetterConsumer {

    private final VoucherOrderTxService voucherOrderTxService;
    private final StockRedisService stockRedisService;
    private final SeckillMetrics metrics;

    @RabbitListener(queues = ORDER_DLQ)
    public void onDeadLetter(OrderMessage message) {
        try {
            if (voucherOrderTxService.existsOrder(message.getOrderId())) {
                // 落库成功但 ACK 丢了：订单是真的，库存已经用掉，不能再还
                log.warn("[死信-已落库] orderId={} 数据库已有订单，仅记录不补偿（ACK 丢失导致的重投）",
                        message.getOrderId());
                metrics.dlqHandled("already_persisted");
                return;
            }
            log.error("[死信-未落库] orderId={} userId={} voucherId={}，回滚 Redis 预扣，需关注根因",
                    message.getOrderId(), message.getUserId(), message.getVoucherId());
            metrics.dlqHandled("rolled_back");
            stockRedisService.rollback(message.getOrderId(), message.getVoucherId(), message.getUserId());
        } catch (Exception e) {
            // 不抛：抛出去消息会被丢弃，问题就从"可人工处理"变成"查不到"
            log.error("[死信-处置失败] orderId={}，消息仍留在 {} 待人工处理",
                    message.getOrderId(), ORDER_DLQ, e);
            metrics.dlqHandled("failed");
        }
    }
}
