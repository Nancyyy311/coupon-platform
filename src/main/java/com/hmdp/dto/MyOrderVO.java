package com.hmdp.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 「我的订单」列表项。
 *
 * 为什么不直接返回 VoucherOrder 实体：
 *   ① 实体里有 userId、payType、update_time 这些前端用不到的字段，返回出去等于扩大接口面；
 *   ② 列表要显示券的标题和面额，而这两项在 tb_voucher 里，实体拼不出来；
 *   ③ 状态是数字（1~6），前端不该自己维护一份"1=未支付"的映射——那是后端的字典，
 *      放在后端改一处就够，放前端就要改两个仓库。
 *
 * 这是 DTO 存在的常态理由：接口契约和数据库表结构本来就不是一回事，
 * 图省事直接返实体，第一次加字段就会把内部结构漏出去。
 */
@Data
@Accessors(chain = true)
public class MyOrderVO {

    /** 订单号（雪花） */
    private Long orderId;

    private Long voucherId;

    /** 券标题；券被删除时给一个可读的兜底文案，不要让前端显示 null */
    private String title;

    /** 售价（分） */
    private Long payValue;

    /** 面额（分） */
    private Long actualValue;

    /** 1 未支付 / 2 已支付 / 3 已核销 / 4 已取消 / 5 退款中 / 6 已退款（本项目未做支付，实际只出现 1 和 4） */
    private Integer status;

    /** 状态文案，由后端给出，前端直接展示 */
    private String statusDesc;

    private LocalDateTime createTime;

    /**
     * 状态字典。
     *
     * 未知状态返回"未知"，而不是抛异常或返回 null：
     * 以后加了新状态（比如 7 已过期），老前端也只是显示"未知"，
     * 不会因为一个没见过的数字整页报错。
     */
    public static String statusDescOf(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1:
                return "未支付";
            case 2:
                return "已支付";
            case 3:
                return "已核销";
            case 4:
                return "已取消";
            case 5:
                return "退款中";
            case 6:
                return "已退款";
            default:
                return "未知";
        }
    }
}
