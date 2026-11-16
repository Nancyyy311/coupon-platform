package com.hmdp.service;

import com.hmdp.dto.MyOrderPage;
import com.hmdp.dto.MyOrderVO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 「我的订单」查询的行为测试。
 *
 * 这里只测真正会出错的地方，不测 MyBatis 会不会分页（那是框架的事）：
 *   · 用户 id 必须来自登录态，不能来自参数（越权）
 *   · size 必须被截断（有人传 10000 会把整张表捞空）
 *   · 券被删除时要有兜底文案（否则前端显示 null）
 *   · 空列表要正常返回，不能 500
 *   · 冷热两表拼接：主表不够要从历史表补，且不能重复、不能因为历史表缺失整页报错
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MyOrderQueryServiceTest {

    private static final long USER_ID = 6L;

    @Mock
    private VoucherOrderMapper voucherOrderMapper;

    @Mock
    private IVoucherService voucherService;

    private MyOrderQueryService service;

    @BeforeEach
    void setUp() {
        service = new MyOrderQueryService(voucherOrderMapper, voucherService);

        UserDTO user = new UserDTO();
        user.setId(USER_ID);
        UserHolder.saveUser(user);

        // 默认：只有主表有数据、历史表为空。单个用例按需覆盖
        when(voucherOrderMapper.countHistoryByUser(USER_ID)).thenReturn(0L);
        when(voucherOrderMapper.selectHistoryByUser(USER_ID, 0L, 10L)).thenReturn(Collections.emptyList());
    }

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    /** 主表：总数 + 一页记录 */
    private void stubHot(long total, List<VoucherOrder> records) {
        when(voucherOrderMapper.countHotByUser(USER_ID)).thenReturn(total);
        when(voucherOrderMapper.selectHotByUser(eq(USER_ID), anyLong(), anyLong())).thenReturn(records);
    }

    private VoucherOrder order(long id, long voucherId, int status) {
        return new VoucherOrder()
                .setId(id)
                .setUserId(USER_ID)
                .setVoucherId(voucherId)
                .setStatus(status)
                .setCreateTime(LocalDateTime.now());
    }

    private Voucher voucher(long id, String title) {
        Voucher v = new Voucher();
        v.setId(id);
        v.setTitle(title);
        v.setPayValue(1000L);
        v.setActualValue(2000L);
        return v;
    }

    private MyOrderPage data(Result result) {
        return (MyOrderPage) result.getData();
    }

    @Test
    @DisplayName("正常返回：订单 + 券标题 + 状态文案，分页字段原样带回")
    void returnsOrdersWithVoucherInfo() {
        stubHot(2, Arrays.asList(
                order(1002L, 7L, 1),
                order(1001L, 8L, 4)));
        when(voucherService.listByIds(anyCollection())).thenReturn(Arrays.asList(
                voucher(7L, "双人套餐"),
                voucher(8L, "单人套餐")));

        Result result = service.queryMyOrders(1, 10);

        assertTrue(result.getSuccess());
        MyOrderPage page = data(result);
        assertEquals(2, page.getTotal());
        assertEquals(2, page.getOrders().size());

        MyOrderVO first = page.getOrders().get(0);
        assertEquals(1002L, first.getOrderId());
        assertEquals("双人套餐", first.getTitle());
        assertEquals("未支付", first.getStatusDesc());
        assertEquals(2000L, first.getActualValue());

        // 状态 4 必须显示"已取消"，不能和"未抢到"混为一谈
        assertEquals("已取消", page.getOrders().get(1).getStatusDesc());
    }

    @Test
    @DisplayName("size 传 10000 要被截断成 20，否则一次请求就能把整张表捞出来")
    void pageSizeIsCapped() {
        stubHot(100, new ArrayList<>());

        MyOrderPage page = data(service.queryMyOrders(1, 10000));

        // 传给 SQL 的 limit 必须是 20，而不是前端传的 10000
        verify(voucherOrderMapper).selectHotByUser(USER_ID, 0L, 20L);
        assertEquals(20L, page.getSize());
    }

    @Test
    @DisplayName("页码非法（0 或负数）要归一到第 1 页，不能让 SQL 出现负 offset")
    void invalidPageFallsBackToFirst() {
        when(voucherOrderMapper.countHotByUser(USER_ID)).thenReturn(100L);
        when(voucherOrderMapper.selectHotByUser(USER_ID, 0L, 10L)).thenReturn(new ArrayList<>());

        service.queryMyOrders(0, 10);
        service.queryMyOrders(-3, 10);

        // 两次都必须是 offset 0
        verify(voucherOrderMapper, org.mockito.Mockito.times(2))
                .selectHotByUser(USER_ID, 0L, 10L);
    }

    @Test
    @DisplayName("没有订单：返回空列表而不是 null，前端不用写空判断")
    void emptyOrders() {
        when(voucherOrderMapper.countHotByUser(USER_ID)).thenReturn(0L);

        Result result = service.queryMyOrders(1, 10);

        assertTrue(result.getSuccess());
        assertEquals(0, data(result).getTotal());
        assertTrue(data(result).getOrders().isEmpty());
        // 没有订单就不该去查券表，省一次 SQL
        verifyNoInteractions(voucherService);
    }

    @Test
    @DisplayName("券已被删除：标题给兜底文案，不能让前端显示 null")
    void voucherMissingUsesFallbackTitle() {
        stubHot(1, Collections.singletonList(order(1003L, 99L, 2)));
        when(voucherService.listByIds(anyCollection())).thenReturn(Collections.emptyList());

        MyOrderVO vo = data(service.queryMyOrders(1, 10)).getOrders().get(0);

        assertEquals("优惠券 #99", vo.getTitle());
        assertEquals("已支付", vo.getStatusDesc());
    }

    @Test
    @DisplayName("未登录直接拒绝，不能因为拿不到用户就查全表")
    void requiresLogin() {
        UserHolder.removeUser();

        Result result = service.queryMyOrders(1, 10);

        assertFalse(result.getSuccess());
        assertEquals("请先登录", result.getErrorMsg());
        verifyNoInteractions(voucherOrderMapper);
    }

    @Test
    @DisplayName("主表不够一页时从历史表补，总数是两张表之和（归档后订单不能消失）")
    void historyFillsTheRestOfThePage() {
        stubHot(1, Collections.singletonList(order(1000L, 7L, 2)));
        when(voucherOrderMapper.countHistoryByUser(USER_ID)).thenReturn(2L);
        when(voucherOrderMapper.selectHistoryByUser(USER_ID, 0L, 9L))
                .thenReturn(Arrays.asList(order(999L, 7L, 3), order(998L, 7L, 4)));
        when(voucherService.listByIds(anyCollection()))
                .thenReturn(Collections.singletonList(voucher(7L, "双人套餐")));

        MyOrderPage page = data(service.queryMyOrders(1, 10));

        assertEquals(3, page.getTotal(), "总数 = 热表 1 + 历史表 2");
        assertEquals(3, page.getOrders().size());
        // 热表在前、历史表在后，与全局 id 倒序一致
        assertEquals(1000L, page.getOrders().get(0).getOrderId());
        assertEquals(999L, page.getOrders().get(1).getOrderId());
        assertEquals(998L, page.getOrders().get(2).getOrderId());
    }

    @Test
    @DisplayName("翻到第二页且已越过热表边界时，直接去历史表取对应偏移")
    void secondPageReadsHistoryWithShiftedOffset() {
        when(voucherOrderMapper.countHotByUser(USER_ID)).thenReturn(3L);
        when(voucherOrderMapper.countHistoryByUser(USER_ID)).thenReturn(10L);
        when(voucherOrderMapper.selectHistoryByUser(USER_ID, 7L, 10L))
                .thenReturn(Collections.singletonList(order(900L, 7L, 4)));
        when(voucherService.listByIds(anyCollection())).thenReturn(Collections.emptyList());

        MyOrderPage page = data(service.queryMyOrders(2, 10));

        assertEquals(13, page.getTotal());
        // offset=10 已越过热表 3 条，历史表偏移应为 7
        assertEquals(1, page.getOrders().size());
        assertEquals(900L, page.getOrders().get(0).getOrderId());
    }

    @Test
    @DisplayName("历史表不存在时降级为只查主表，不能让整页 500")
    void missingHistoryTableDegradesToHotOnly() {
        stubHot(1, Collections.singletonList(order(1000L, 7L, 2)));
        when(voucherOrderMapper.countHistoryByUser(USER_ID))
                .thenThrow(new RuntimeException("Table 'tb_voucher_order_history' doesn't exist"));
        when(voucherService.listByIds(anyCollection())).thenReturn(Collections.emptyList());

        Result result = service.queryMyOrders(1, 10);

        assertTrue(result.getSuccess());
        assertEquals(1, data(result).getTotal());
        assertEquals(1, data(result).getOrders().size());
        // 总数已经是 0，就不该再发一次注定失败的查询
        verify(voucherOrderMapper, never()).selectHistoryByUser(eq(USER_ID), anyLong(), anyLong());
    }

    @Test
    @DisplayName("归档中断过（同一单同时存在于两张表）不能重复展示")
    void duplicateIdAcrossTablesIsCollapsed() {
        stubHot(1, Collections.singletonList(order(1000L, 7L, 4)));
        when(voucherOrderMapper.countHistoryByUser(USER_ID)).thenReturn(1L);
        when(voucherOrderMapper.selectHistoryByUser(USER_ID, 0L, 9L))
                .thenReturn(Collections.singletonList(order(1000L, 7L, 4)));
        when(voucherService.listByIds(anyCollection())).thenReturn(Collections.emptyList());

        MyOrderPage page = data(service.queryMyOrders(1, 10));

        assertEquals(1, page.getOrders().size(), "同一个 orderId 只能出现一次");
        assertEquals(1000L, page.getOrders().get(0).getOrderId());
    }
}
