package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.VoucherOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {

    /**
     * 下面四个方法是「我的订单」用的。
     *
     * 为什么不直接用 selectPage：订单做了冷热分离（见 OrderArchiveJob），
     * 90 天前的终态订单会被搬到 tb_voucher_order_history。
     * 只查主表的话，用户翻自己的历史订单会"凭空少了几单"——
     * 数据没丢，但用户看不到，效果和丢了是一样的。
     *
     * 冷热两张表按 id 倒序拼页：热表整体比冷表新（归档只搬 90 天前的），
     * 所以"先热后冷"的拼接顺序与全局 id 倒序一致，不需要 UNION 再排序，
     * 也就不会触发"派生表 + filesort"那类深分页问题。
     */
    @Select("SELECT COUNT(*) FROM tb_voucher_order WHERE user_id = #{userId}")
    long countHotByUser(@Param("userId") Long userId);

    @Select("SELECT id, user_id, voucher_id, pay_type, status, create_time, pay_time, use_time, refund_time, update_time "
            + "FROM tb_voucher_order WHERE user_id = #{userId} ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<VoucherOrder> selectHotByUser(@Param("userId") Long userId,
                                      @Param("offset") long offset,
                                      @Param("limit") long limit);

    @Select("SELECT COUNT(*) FROM tb_voucher_order_history WHERE user_id = #{userId}")
    long countHistoryByUser(@Param("userId") Long userId);

    @Select("SELECT id, user_id, voucher_id, pay_type, status, create_time, pay_time, use_time, refund_time, update_time "
            + "FROM tb_voucher_order_history WHERE user_id = #{userId} ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<VoucherOrder> selectHistoryByUser(@Param("userId") Long userId,
                                          @Param("offset") long offset,
                                          @Param("limit") long limit);
}
