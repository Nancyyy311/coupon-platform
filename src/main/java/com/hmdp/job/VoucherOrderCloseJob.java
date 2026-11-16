package com.hmdp.job;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.VoucherOrderCloseService;
import com.hmdp.utils.JobLock;
import com.hmdp.utils.StockRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 超时关单任务（高可用）。
 *
 * 调度用 @Scheduled + Redis 任务锁，没有引分布式调度中心，理由：
 *   这个任务本身是幂等的——关单靠 "WHERE id=? AND status=1" 的条件更新，
 *   两个节点同时扫到同一笔订单，只有一个能改成 4，另一个影响行数为 0 直接退出，
 *   库存也只会回补一次。所以**幂等才是正确性保障**，锁不是。
 *
 *   那为什么还要加锁：不加锁时两个实例每 60 秒各扫一次全表、各打一条
 *   "本批关闭 N 笔"的日志，而实际只有一边真正关了单。多实例一多，
 *   扫描量和日志噪音线性增长，排查问题时反而看不出真实关单量。
 *   锁的作用是"省重复扫描 + 让日志可信"，不是"防重复关单"。
 *
 * 什么时候需要调度中心：任务本身不幂等（比如发短信、调外部接口），
 *   或者需要人工触发、需要执行历史、需要失败重跑记录。本项目三个任务都幂等，
 *   为它引入 XXL-Job（还要单独部署调度中心 + 建库）不划算。
 *
 * 为什么在 closeOne 之后单独调 Redis 回补：
 *   数据库事务提交之后才动 Redis。放在事务里的话，一旦事务回滚，
 *   Redis 的补偿就成了无法解释的脏数据（库存多了但订单还在）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.close-job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VoucherOrderCloseJob {

    private final VoucherOrderCloseService closeService;
    private final StockRedisService stockRedisService;
    private final JobLock jobLock;

    private static final String JOB_NAME = "order-close";

    /** 锁 TTL 取调度间隔的 2 倍：正常几秒就结束；崩溃时最坏多等 2 分钟，下一轮自动恢复 */
    private static final long LOCK_TTL_SECONDS = 120L;

    /** 每 60 秒扫一次，启动后延迟 30 秒，避开应用启动时的资源竞争 */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void closeTimeoutOrders() {
        // null = 别的实例正在扫，本次跳过；空串 = Redis 不可用降级执行（回补本来也要用 Redis，
        // 拿不到锁也做不了什么，但让它跑一遍比直接不跑更容易发现问题）
        String token = jobLock.tryAcquire(JOB_NAME, LOCK_TTL_SECONDS);
        if (token == null) {
            return;
        }
        try {
            doClose();
        } finally {
            jobLock.release(JOB_NAME, token);
        }
    }

    private void doClose() {
        List<VoucherOrder> timeoutOrders;
        try {
            timeoutOrders = closeService.queryTimeoutOrders();
        } catch (Exception e) {
            log.error("[超时关单] 查询失败", e);
            return;
        }
        if (timeoutOrders.isEmpty()) {
            return;
        }

        int closed = 0;
        for (VoucherOrder order : timeoutOrders) {
            try {
                // 这里必须通过 closeService 调用，才能走到 Spring 代理，事务才会生效
                if (closeService.closeOne(order)) {
                    // 数据库已是终态，再补 Redis；失败也不影响关单结果，由对账任务兜底。
                    // 传 orderId 是因为回补脚本用它做幂等键：关单与"消息重投导致的回滚"
                    // 有可能指向同一笔订单，重复回补会直接变成超卖
                    stockRedisService.rollback(order.getId(), order.getVoucherId(), order.getUserId());
                    closed++;
                }
            } catch (Exception e) {
                // 单笔失败不影响整批，下一轮会重新扫到
                log.error("[超时关单] orderId={} 处理失败", order.getId(), e);
            }
        }

        if (closed > 0) {
            log.warn("[超时关单] 本批关闭 {} 笔未支付订单，库存已回补", closed);
        }
    }
}
