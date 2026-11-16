package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.MyOrderQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private MyOrderQueryService myOrderQueryService;

    /**
     * 秒杀下单。同步部分只做 Redis 预扣 + 投递消息，落库由消费者异步完成，
     * 所以这里返回的是订单号，而不是订单详情。
     */
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {

        return voucherOrderService.seckillVoucher(voucherId);
    }

    /**
     * 按订单号查询秒杀结果。
     *
     * 异步下单必须配这个接口：前端拿到订单号后拿它轮询，直到 state 不再是 PENDING。
     * state 有四种取值（PENDING / CREATED / CLOSED / FAILED），
     * 前端不能自己把 PENDING 当成失败，否则会把还在处理中的订单误判为"没抢到"。
     */
    @GetMapping("result/{orderId}")
    public Result querySeckillResult(@PathVariable("orderId") Long orderId) {

        return voucherOrderService.querySeckillResult(orderId);
    }

    /**
     * 我的订单（分页）。
     *
     * 为什么需要它：下单只返回一个订单号，结果查询也只回答"这一单成没成"。
     * 用户关掉页面之后，就再也找不到自己的订单了——这不是体验问题，
     * 是链路缺了最后一段：抢到的东西必须能被重新找到。
     *
     * 用户 id 从登录态取，不接受前端传入，避免越权查别人的订单。
     *
     * @param current 页码，从 1 开始
     * @param size    每页条数，上限 20（后端截断，防止 size 传太大把表捞空）
     */
    @GetMapping("my")
    public Result queryMyOrders(@RequestParam(value = "current", defaultValue = "1") Integer current,
                                @RequestParam(value = "size", defaultValue = "10") Integer size) {

        return myOrderQueryService.queryMyOrders(current, size);
    }
}
