package com.hmdp.service;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.mq.OrderMessage;
import com.hmdp.mq.StockNotEnoughException;
import com.hmdp.service.ISeckillVoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 订单落库（高可用）。
 *
 * 这个类单独存在的唯一原因：事务注解必须经过 Spring 代理才生效。
 * 原版把它写在 VoucherOrderServiceImpl 里、用 this 调用，@Transactional 是失效的
 * （扣了库存但插单失败时事务不会回滚）。
 *
 * 职责边界：只做数据库动作，不碰 Redis、不碰 MQ。补偿与重试由上层负责。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherOrderTxService {

    private final VoucherOrderMapper voucherOrderMapper;
    private final ISeckillVoucherService seckillVoucherService;

    /**
     * 落单：幂等 + 条件扣库存 + 插入订单，三件事在同一个事务里。
     *
     * @throws StockNotEnoughException 数据库库存不足（Redis 与 DB 不一致）
     */
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(OrderMessage message) {
        // 1. 按订单号幂等：消息重复投递时直接返回，不会产生第二单
        if (voucherOrderMapper.selectById(message.getOrderId()) != null) {
            log.info("[订单已存在，跳过] orderId={}", message.getOrderId());
            return;
        }

        // 2. 数据库扣库存。条件更新保证不会扣成负数，
        //    这条语句同时也是"最终防超卖"的那道闸，不依赖 Redis 是否可信
        boolean deducted = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", message.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!deducted) {
            throw new StockNotEnoughException(message.getVoucherId());
        }

        // 3. 插入订单。uk_user_voucher 唯一索引是"一人一单"的最后防线：
        //    Redis 的 Set 可能因为重启或主从切换丢失，索引不会
        VoucherOrder order = new VoucherOrder();
        order.setId(message.getOrderId());
        order.setUserId(message.getUserId());
        order.setVoucherId(message.getVoucherId());
        order.setStatus(1); // 1 未支付
        order.setCreateTime(toLocalDateTime(message.getCreateTimeMillis()));
        order.setUpdateTime(LocalDateTime.now());
        voucherOrderMapper.insert(order);
    }

    /**
     * 这笔订单是否已经落库。
     *
     * 只在唯一索引冲突的异常路径上调用，用来区分两种完全不同的成因：
     *   订单已存在 -> 同一条消息被并发消费，库存已被那次扣过，不能重复归还
     *   订单不存在 -> 是该用户已有另一笔有效订单，本次多扣的库存要还
     * 主链路不调用，所以不增加任何正常路径上的数据库开销。
     */
    public boolean existsOrder(Long orderId) {
        return orderId != null && voucherOrderMapper.selectById(orderId) != null;
    }

    private LocalDateTime toLocalDateTime(Long millis) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(millis == null ? System.currentTimeMillis() : millis),
                ZoneId.systemDefault()
        );
    }
}
