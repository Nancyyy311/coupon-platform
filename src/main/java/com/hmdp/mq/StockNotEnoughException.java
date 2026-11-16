package com.hmdp.mq;

/**
 * 数据库库存扣减失败。
 * 触发条件是"Redis 说还有库存，但数据库说没有"——这是数据不一致信号，
 * 不重试、不阻塞队列，交由对账任务定位差异。
 */
public class StockNotEnoughException extends RuntimeException {
    public StockNotEnoughException(Long voucherId) {
        super("数据库库存不足，voucherId=" + voucherId + "，Redis 与 DB 不一致");
    }
}
