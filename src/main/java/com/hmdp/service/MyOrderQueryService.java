package com.hmdp.service;

import com.hmdp.dto.MyOrderPage;
import com.hmdp.dto.MyOrderVO;
import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 「我的订单」查询（只读）。
 *
 * 为什么单独一个类，不塞进 VoucherOrderServiceImpl：
 *   下单那条路径是热路径，每个请求只碰 Redis + MQ，一个多余的依赖都不该有；
 *   这里是后台查询，依赖数据库和券表。两者放在一个类里，热路径的构造依赖会被读请求带脏。
 *   读写分开之后，下单类的构造函数保持"只依赖下单需要的东西"，也更好测。
 *
 * 为什么按主键倒序而不是 create_time 倒序：
 *   订单号是雪花，趋势递增且与创建时间同序；按主键排直接走聚簇索引，
 *   create_time 上没有索引，用它排序会多一次排序步骤。
 *
 * 为什么要查两张表（这里是最容易漏的一处）：
 *   OrderArchiveJob 会把 90 天前的终态订单搬到 tb_voucher_order_history，
 *   只查主表的话，用户翻自己的历史订单会"少几单"。数据没丢，但看不到，等于丢了。
 *   冷热拼接的做法见 queryMyOrders 里的注释。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MyOrderQueryService {

    /** 单页上限：前端传 size=10000 时也不能把整表捞出来 */
    private static final int MAX_PAGE_SIZE = 20;

    private static final int DEFAULT_PAGE_SIZE = 10;

    /** 历史表缺失只在第一次告警，避免每来一个请求刷一条日志 */
    private final AtomicBoolean historyTableWarned = new AtomicBoolean(false);

    private final VoucherOrderMapper voucherOrderMapper;
    private final IVoucherService voucherService;

    /**
     * 查询当前登录用户的订单，按时间倒序分页。
     *
     * 用户 id 一律从登录态取，绝不接受前端传参——
     * 一旦 allow 传 userId，就等于任意用户都能查别人的订单，是典型的越权漏洞。
     *
     * 分页是"冷热两段拼"：先定位在主表的区间，主表不够再从历史表补。
     * 不用 UNION 的理由：UNION 的 COUNT 要包一层派生表，分页时 MySQL 会把两表数据
     * 全物化再排序，深分页会退化成临时表 + filesort。分开查各有主键/索引可用，
     * 每段都是 LIMIT OFFSET 的索引扫描。
     */
    public Result queryMyOrders(Integer current, Integer size) {
        if (UserHolder.getUser() == null) {
            return Result.fail("请先登录");
        }
        Long userId = UserHolder.getUser().getId();

        long pageNo = current == null || current < 1 ? 1L : current;
        long pageSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        long offset = (pageNo - 1) * pageSize;

        // 两张表各自的总数：热表直接查（主键前缀 user_id 走 uk_user_voucher），
        // 历史表缺失时按 0 处理，不能让"没有历史表"把整个列表打成 500
        long hotTotal = voucherOrderMapper.countHotByUser(userId);
        long coldTotal = safeCountHistory(userId);

        List<VoucherOrder> records = new ArrayList<>((int) pageSize);
        if (offset < hotTotal) {
            records.addAll(voucherOrderMapper.selectHotByUser(userId, offset, pageSize));
        }
        // 主表这段没填满，说明这一页跨到了历史表
        if (records.size() < pageSize) {
            long coldOffset = Math.max(0, offset - hotTotal);
            long coldLimit = pageSize - records.size();
            if (coldOffset < coldTotal) {
                records.addAll(safeSelectHistory(userId, coldOffset, coldLimit));
            }
        }

        // 归档任务中断过的话（历史表插成功、主表没删），同一个 id 会在两张表里各有一份。
        // 按 id 去重，避免用户看到重复订单；顺带保住"热表在前"的顺序。
        List<VoucherOrder> ordered = dedupeById(records);

        Map<Long, Voucher> voucherMap = loadVouchers(ordered);

        List<MyOrderVO> orders = new ArrayList<>(ordered.size());
        for (VoucherOrder order : ordered) {
            Voucher voucher = voucherMap.get(order.getVoucherId());
            orders.add(new MyOrderVO()
                    .setOrderId(order.getId())
                    .setVoucherId(order.getVoucherId())
                    // 券被删除时给可读兜底，前端不用为 null 单独写分支
                    .setTitle(voucher == null ? "优惠券 #" + order.getVoucherId() : voucher.getTitle())
                    .setPayValue(voucher == null ? null : voucher.getPayValue())
                    .setActualValue(voucher == null ? null : voucher.getActualValue())
                    .setStatus(order.getStatus())
                    .setStatusDesc(MyOrderVO.statusDescOf(order.getStatus()))
                    .setCreateTime(order.getCreateTime()));
        }

        return Result.ok(new MyOrderPage()
                .setTotal(hotTotal + coldTotal)
                .setCurrent(pageNo)
                .setSize(pageSize)
                .setOrders(orders));
    }

    /** 顺序去重：id 相同只保留第一次出现的那条（主表在前的顺序不变） */
    private List<VoucherOrder> dedupeById(List<VoucherOrder> records) {
        if (records.isEmpty()) {
            return records;
        }
        Map<Long, VoucherOrder> unique = new LinkedHashMap<>(records.size() * 2);
        for (VoucherOrder order : records) {
            unique.putIfAbsent(order.getId(), order);
        }
        return new ArrayList<>(unique.values());
    }

    private long safeCountHistory(Long userId) {
        try {
            return voucherOrderMapper.countHistoryByUser(userId);
        } catch (Exception e) {
            warnHistoryTableOnce(e);
            return 0L;
        }
    }

    private List<VoucherOrder> safeSelectHistory(Long userId, long offset, long limit) {
        try {
            return voucherOrderMapper.selectHistoryByUser(userId, offset, limit);
        } catch (Exception e) {
            warnHistoryTableOnce(e);
            return Collections.emptyList();
        }
    }

    /**
     * 历史表不存在时只降级、不报错。
     *
     * 为什么允许它不存在：本地库是照着 hmdp.sql + V1__upgrade.sql 建的，
     * 有人只跑了主脚本、没跑升级脚本，历史表就缺。
     * 这种情况下"当前订单"仍然要能看，只是看不到已归档的——比整页 500 好。
     * 但必须留一条 warn，否则真的归档失败时没人知道。
     */
    private void warnHistoryTableOnce(Exception e) {
        if (historyTableWarned.compareAndSet(false, true)) {
            log.warn("[我的订单] 读取历史订单表失败，已降级为只查当前表。"
                    + "请确认已执行 db/V1__upgrade.sql（建 tb_voucher_order_history）", e);
        }
    }

    /**
     * 批量取券信息，避免 N+1：一页 10 条订单如果逐条查券，就是 11 次 SQL。
     * 券查不到（被删）不影响订单展示，所以这里不抛异常，交给上层做兜底文案。
     */
    private Map<Long, Voucher> loadVouchers(List<VoucherOrder> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> ids = new HashSet<>();
        for (VoucherOrder order : orders) {
            if (order.getVoucherId() != null) {
                ids.add(order.getVoucherId());
            }
        }
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Voucher> vouchers = voucherService.listByIds(ids);
        Map<Long, Voucher> map = new HashMap<>(vouchers.size() * 2);
        for (Voucher voucher : vouchers) {
            map.put(voucher.getId(), voucher);
        }
        return map;
    }
}
