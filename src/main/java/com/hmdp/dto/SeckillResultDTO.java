package com.hmdp.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 秒杀结果查询的返回体。
 *
 * 为什么必须有这个接口：
 *   下单接口在收到消息后立刻返回订单号，落库是异步的。如果前端只有"下单"没有"查询"，
 *   用户看到的是一个单号，但不知道这单成没成——链路是断的。
 *   Stream/RabbitMQ 异步化之后，"查询结果"不是可选功能，是这套架构的必要配套。
 *
 * 四个状态的区别（这是接口设计的核心，不能糊成一个"成功/失败"）：
 *
 *   PENDING  数据库没有单、Redis 也没有回滚标记 -> 消息还在路上
 *            此时既不能说成功也不能说失败，只能让前端稍后再查。
 *            如果把 PENDING 当失败返回，用户会以为没抢到，重复下单，反而制造无效流量。
 *
 *   CREATED  数据库有单，且状态有效（未支付/已支付/已核销/退款中）
 *
 *   CLOSED   数据库有单，但状态是"已取消"（超时未支付被关单）
 *            注意它和 FAILED 不同：这单曾经真的抢到了，只是后来过期了。
 *            前端文案应该是"订单已超时取消"，不是"没抢到"。
 *
 *   FAILED   数据库没有单，但 Redis 里存在该订单号的回滚标记
 *            说明预扣之后被回滚了（MQ 发送失败 / 消费失败进死信 / 被唯一索引拦下）
 *            这才是真正的"没抢到"。
 *
 *   TIMEOUT  数据库没有单、也没有回滚标记，但订单号生成至今已超过阈值（默认 60 秒）
 *            含义是"消息确实丢了"：预扣成功之后进程被 kill、broker 丢消息、
 *            消息进了某个没人消费的队列，都会走到这里。它和 PENDING 的区别只有时间。
 *
 *            为什么必须有这个状态：PENDING 是唯一一个"没有出口"的状态。
 *            没有它，用户会永远看到"正在处理中"，反复刷新，而系统里没有任何东西会变。
 *            任何非终态都必须有人把它推到终态，这是异步链路的基本纪律。
 */
@Data
@Accessors(chain = true)
public class SeckillResultDTO {

    /** 消息还在处理中，前端应按 retryAfterMs 继续轮询 */
    public static final String STATE_PENDING = "PENDING";
    /** 订单已创建，看 orderStatus 判断具体阶段 */
    public static final String STATE_CREATED = "CREATED";
    /** 订单已超时取消 */
    public static final String STATE_CLOSED = "CLOSED";
    /** 未抢到，预扣已回滚 */
    public static final String STATE_FAILED = "FAILED";
    /** 超过等待阈值仍未落库：消息很可能已经丢了，让用户重试而不是无限等待 */
    public static final String STATE_TIMEOUT = "TIMEOUT";

    private Long orderId;

    private String state;

    /** 订单状态，1 未支付 / 2 已支付 / 3 已核销 / 4 已取消 / 5 退款中 / 6 已退款（本项目未做支付，实际只出现 1 和 4） */
    private Integer orderStatus;

    private LocalDateTime createTime;

    /** 仅 PENDING 时有意义：建议前端多久之后再查一次（毫秒） */
    private Long retryAfterMs;

    /** 给用户看的文案，前端可直接展示 */
    private String msg;
}
