package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券订单（tb_voucher_order）。
 *
 * <p><b>先说清这个"订单"是什么</b>：它是"抢券成功"的记录，不是一笔交易。
 * 本项目没有支付模块——没有收银台、没有支付回调、没有核销入口、没有退款流程，
 * 所以 {@code payType / payTime / useTime / refundTime} 四个字段<b>全代码零写入，恒为 NULL</b>。
 * 它们是从课程基线表结构继承下来的列，保留是为了不改动线上已有表结构，
 * 不代表这里实现过资金流。
 *
 * <p><b>实际可能出现的状态只有两个</b>：
 * <ul>
 *   <li>{@code 1 未支付} —— 秒杀落库时的初始状态，也是唯一会被主动写入的状态</li>
 *   <li>{@code 4 已取消} —— 超过 15 分钟仍"未支付"，被 {@code VoucherOrderCloseJob} 扫表关掉</li>
 * </ul>
 * 状态字典里的 {@code 2 已支付 / 3 已核销 / 5 退款中 / 6 已退款} 没有任何代码路径能到达。
 * 字典保留它们，是为了接支付时不改协议、老前端也不会因为陌生状态崩掉。
 *
 * <p>答辩时正确说法：<b>"订单是领券记录，业务上没做支付，状态只有未支付和超时取消两条路径"</b>。
 * 不要说"有真实订单"，那会立刻被追问支付、退款、对账，三个都答不上来。
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher_order")
public class VoucherOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 下单的用户id
     */
    private Long userId;

    /**
     * 购买的代金券id
     */
    private Long voucherId;

    /**
     * 支付方式 1：余额支付；2：支付宝；3：微信
     *
     * <p>本项目未实现支付，此字段从不写入，恒为 NULL。保留仅为对齐课程基线表结构。
     */
    private Integer payType;

    /**
     * 订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款
     *
     * <p>实际只有 1 和 4 可达：落库写 1，超时关单写 4。2/3/5/6 无代码路径。
     */
    private Integer status;

    /**
     * 下单时间
     */
    private LocalDateTime createTime;

    /**
     * 支付时间（未实现支付，恒为 NULL）
     */
    private LocalDateTime payTime;

    /**
     * 核销时间（未实现核销，恒为 NULL）
     */
    private LocalDateTime useTime;

    /**
     * 退款时间（未实现退款，恒为 NULL）
     */
    private LocalDateTime refundTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
