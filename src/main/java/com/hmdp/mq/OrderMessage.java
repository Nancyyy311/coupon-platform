package com.hmdp.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 下单消息体。
 * 只带最小必要字段：消费者拿到它能独立完成落库，不依赖生产者的任何上下文。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单号（雪花生成，生产端确定，消费端直接用作主键，保证重投不产生新单） */
    private Long orderId;

    private Long userId;

    private Long voucherId;

    /** 下单时间戳，消费端用它写 create_time，而不是用消费时刻 */
    private Long createTimeMillis;
}
