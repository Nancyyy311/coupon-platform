package com.hmdp.cache;

import lombok.Getter;

/**
 * 商户数据变更事件。
 *
 * 为什么不直接在 update 里删缓存：事务提交是在方法返回之后。
 * 在事务里删缓存并广播，其他实例收到通知后立刻回源查库——查的是**未提交**的数据，
 * 读到旧值又被写回缓存（TTL 30 分钟），于是一次更新换来半小时的脏读。
 *
 * 所以改成"发事件 + 提交后消费"：
 *   @TransactionalEventListener(phase = AFTER_COMMIT) 只在提交成功时触发，回滚时自动不执行。
 *   这比手写 TransactionSynchronizationManager 少一半代码，也更不容易漏。
 *
 * 为什么还要带上变更前的 typeId / x / y：
 *   GEO 索引按分类分 key（shop:geo:{typeId}），换分类意味着"从旧 key 移走 + 写进新 key"。
 *   提交后再查库只能查到新值，旧分类无从得知，所以必须在事务内把旧值捎出来。
 *   坐标同理：不比对旧值，每次改名都要重写一遍 GEO。
 */
@Getter
public class ShopChangedEvent {

    /** 变更的商户 id */
    private final Long shopId;

    /** 变更前的分类，新增时为 null */
    private final Long oldTypeId;

    /** 变更前的坐标，新增时为 null */
    private final Double oldX;

    private final Double oldY;

    public ShopChangedEvent(Long shopId) {
        this(shopId, null, null, null);
    }

    public ShopChangedEvent(Long shopId, Long oldTypeId, Double oldX, Double oldY) {
        this.shopId = shopId;
        this.oldTypeId = oldTypeId;
        this.oldX = oldX;
        this.oldY = oldY;
    }
}
