package com.hmdp.mq;

import com.hmdp.service.VoucherOrderTxService;
import com.hmdp.utils.StockRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import static com.hmdp.config.RabbitMQConfig.ORDER_QUEUE;

/**
 * 下单消息消费者（高可用）。
 *
 * 失败分类处理，这是原版 Redis Stream 做不到的地方：
 *   - 库存不一致 / 重复下单：业务层面的"确定性失败"，重试一万次也一样，
 *     直接吞掉（ACK），否则只会把队列堵死
 *   - 其他异常（数据库连接闪断、锁等待超时）：临时性失败，抛出去让容器按
 *     retry 配置重试，重试用尽后自动转入死信队列，而不是永远堆在 pending
 *
 * 关键：吞掉消息之前必须把 Redis 的预扣还回去。
 *   这里第一版就是错的——catch 里只打了一行日志就 ACK 了，Redis 那份库存
 *   永远回不来，占位也永远清不掉，用户之后每次点都只会看到"每人限购一张"。
 *   对账任务虽然能报出"幽灵占位"，但它只告警不自动修（因为分不清消息是不是
 *   还在队列里），最后只能人工处理。所以确定性失败必须当场补偿。
 *
 * 并发消费：容器 concurrentConsumers / maxConcurrentConsumers 可配（见 yaml），
 * 原版单线程 Executors.newSingleThreadExecutor() 的吞吐上限就锁死在一个线程上。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final VoucherOrderTxService voucherOrderTxService;
    private final StockRedisService stockRedisService;

    @RabbitListener(queues = ORDER_QUEUE)
    public void onMessage(OrderMessage message) {
        long start = System.currentTimeMillis();
        try {
            voucherOrderTxService.createOrder(message);
            log.info("[下单成功] orderId={} userId={} voucherId={} cost={}ms",
                    message.getOrderId(), message.getUserId(), message.getVoucherId(),
                    System.currentTimeMillis() - start);
        } catch (DuplicateKeyException e) {
            handleDuplicate(message, e);
        } catch (StockNotEnoughException e) {
            // 数据库说没库存，Redis 说还有。这条消息注定失败，ACK 掉避免堵队列，
            // 但必须先把这个用户占的那一份还回去：他没拿到券，不该被占位卡住
            log.error("[库存不一致] orderId={} voucherId={}，回滚 Redis 预扣，差异交由对账",
                    message.getOrderId(), message.getVoucherId());
            stockRedisService.rollback(message.getOrderId(), message.getVoucherId(),
                    message.getUserId(), StockRedisService.MODE_FULL);
        }
    }

    /**
     * 唯一索引冲突有两种完全不同的成因，处理方式相反，必须分开：
     *
     *   ① 主键冲突：同一条消息被并发消费（MQ 至少一次投递 + 多消费者）。
     *      另一个线程正在/已经插入这笔订单，库存也被它扣过了。
     *      此时再回补 = 库存多 1 = 超卖。只能什么都不做。
     *
     *   ② uk_user_voucher 冲突：该用户已有有效订单（Redis 的 Set 因为重启或
     *      主从切换丢了，Lua 以为他没抢过，于是又扣了一次库存）。
     *      他没拿到新券，多扣的那 1 份要还，但占位必须保留——
     *      否则他会一直抢、一直被唯一索引拦，形成死循环。
     *
     * 用"这笔订单是否已经落库"来区分，比解析异常 message 里的索引名可靠。
     */
    private void handleDuplicate(OrderMessage message, DuplicateKeyException e) {
        if (voucherOrderTxService.existsOrder(message.getOrderId())) {
            log.warn("[重复投递已跳过] orderId={} 已落库，不回补库存", message.getOrderId());
            return;
        }
        log.warn("[一人一单拦截] userId={} voucherId={}，回补多扣的库存（保留占位）",
                message.getUserId(), message.getVoucherId());
        stockRedisService.rollback(message.getOrderId(), message.getVoucherId(),
                message.getUserId(), StockRedisService.MODE_STOCK_ONLY);
    }
}
