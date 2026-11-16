package com.hmdp.service.impl;

import com.hmdp.cache.LocalSoldOutGuard;
import com.hmdp.dto.Result;
import com.hmdp.dto.SeckillResultDTO;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.monitor.SeckillMetrics;
import com.hmdp.mq.OrderMessage;
import com.hmdp.mq.OrderProducer;
import com.hmdp.service.VoucherOrderTxService;
import com.hmdp.utils.SnowflakeIdWorker;
import com.hmdp.utils.TokenBucketRateLimiter;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 秒杀主链路的行为测试（纯 Mockito，不启 Spring、不连 Redis，整套跑完 < 1s）。
 *
 * 为什么要有这组测试：
 *   这条链路有 6 个出口（售罄 / 重复 / 未开始 / 已结束 / 未预热 / 投递失败），
 *   每个出口对应一句给用户看的文案和一个监控标签。文案写错、标签打错，
 *   接口不会报错，测试也不会红——只有线上被用户投诉才会发现。
 *   所以这里用"短路一个依赖、断言一个结果"的方式把每个出口钉死。
 *
 * 这类测试能回答面试里最实际的问题：
 *   "你改了这么多，怎么保证没把原来能跑的路径改坏？"
 *   答案不是"我手动点了一遍"，而是"6 个分支都有测试，改一处红一处"。
 */
@ExtendWith(MockitoExtension.class)
class VoucherOrderServiceImplTest {

    private static final long VOUCHER_ID = 7L;
    private static final long USER_ID = 1L;
    private static final long ORDER_ID = 1234567890123L;

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private SnowflakeIdWorker snowflakeIdWorker;
    @Mock
    private OrderProducer orderProducer;
    @Mock
    private LocalSoldOutGuard localSoldOutGuard;
    @Mock
    private VoucherOrderTxService voucherOrderTxService;
    @Mock
    private TokenBucketRateLimiter rateLimiter;
    @Mock
    private SeckillMetrics metrics;

    private VoucherOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VoucherOrderServiceImpl(
                stringRedisTemplate, snowflakeIdWorker, orderProducer,
                localSoldOutGuard, voucherOrderTxService, rateLimiter, metrics);

        UserDTO user = new UserDTO();
        user.setId(USER_ID);
        UserHolder.saveUser(user);

        // 默认放行：只有专门测限流的用例才覆盖这个桩。
        // lenient() 是因为部分用例在限流之前就返回了，不会走到这里。
        lenient().when(rateLimiter.acquire(anyString(), anyLong()))
                .thenReturn(TokenBucketRateLimiter.Decision.ALLOWED);
        lenient().when(snowflakeIdWorker.nextId()).thenReturn(ORDER_ID);
    }

    @AfterEach
    void tearDown() {
        // ThreadLocal 不清会污染下一个用例（同一个线程复用）
        UserHolder.removeUser();
    }

    /** 让 Lua 脚本返回指定返回码。execute 是泛型 + 可变参数，用 doReturn 避开类型推断 */
    private void stubLua(int code) {
        doReturn((long) code).when(stringRedisTemplate)
                .execute(any(RedisScript.class), ArgumentMatchers.<String>anyList(), any(), any());
    }

    @Test
    @DisplayName("本机已标记售罄：直接返回，连 Redis 和限流器都不碰")
    void localSoldOutShortCircuits() {
        when(localSoldOutGuard.isSoldOut(VOUCHER_ID)).thenReturn(true);

        Result result = service.seckillVoucher(VOUCHER_ID);

        assertFalse(result.getSuccess());
        assertEquals("已售罄", result.getErrorMsg());
        // 这两条是本用例的核心：本地标识的意义就是"卖完后零 Redis 命令"
        verifyNoInteractions(stringRedisTemplate);
        verifyNoInteractions(rateLimiter);
        verify(metrics).localSoldOutBlock();
    }

    @Test
    @DisplayName("本地窗口限流拦下：不进入 Redis 预扣")
    void localRateLimited() {
        when(rateLimiter.acquire("seckill", USER_ID))
                .thenReturn(TokenBucketRateLimiter.Decision.LIMITED_LOCAL);

        Result result = service.seckillVoucher(VOUCHER_ID);

        assertFalse(result.getSuccess());
        assertEquals("手速太快了，请稍后再试", result.getErrorMsg());
        verifyNoInteractions(stringRedisTemplate);
        verify(metrics).seckillResult(SeckillMetrics.RESULT_RATE_LIMITED_LOCAL);
    }

    @Test
    @DisplayName("Redis 令牌桶限流：本地放过、Redis 拦下，标签要打到 rate_limited 而不是 local")
    void redisRateLimited() {
        when(rateLimiter.acquire("seckill", USER_ID))
                .thenReturn(TokenBucketRateLimiter.Decision.LIMITED_REDIS);

        Result result = service.seckillVoucher(VOUCHER_ID);

        assertFalse(result.getSuccess());
        verifyNoInteractions(stringRedisTemplate);
        verify(metrics).seckillResult(SeckillMetrics.RESULT_RATE_LIMITED);
    }

    @Test
    @DisplayName("Lua 返回 1（库存不足）：要给本机打售罄标记，否则卖完后每个请求还会打 Redis")
    void soldOutFromRedisMarksLocal() {
        stubLua(1);

        Result result = service.seckillVoucher(VOUCHER_ID);

        assertFalse(result.getSuccess());
        assertEquals("已售罄", result.getErrorMsg());
        verify(localSoldOutGuard).markSoldOut(VOUCHER_ID);
        verify(orderProducer, never()).send(any());
    }

    @Test
    @DisplayName("Lua 返回 2（一人一单）：返回限购文案，不能误打成售罄")
    void duplicateOrder() {
        stubLua(2);

        Result result = service.seckillVoucher(VOUCHER_ID);

        assertFalse(result.getSuccess());
        assertEquals("每人限购一张", result.getErrorMsg());
        // 重复下单不是售罄，绝不能打本地售罄标记
        verify(localSoldOutGuard, never()).markSoldOut(anyLong());
        verify(metrics).seckillResult(SeckillMetrics.RESULT_DUPLICATE);
    }

    @Test
    @DisplayName("Lua 返回 3（库存未预热）：文案是'太火爆'，且不能打售罄标记")
    void notReady() {
        stubLua(3);

        Result result = service.seckillVoucher(VOUCHER_ID);

        assertFalse(result.getSuccess());
        assertEquals("活动太火爆了，请稍后再试", result.getErrorMsg());
        verify(localSoldOutGuard, never()).markSoldOut(anyLong());
        verify(metrics).seckillResult(SeckillMetrics.RESULT_NOT_READY);
    }

    @Test
    @DisplayName("Lua 返回 4（未开始）")
    void notStart() {
        stubLua(4);

        Result result = service.seckillVoucher(VOUCHER_ID);

        assertFalse(result.getSuccess());
        assertEquals("活动未开始", result.getErrorMsg());
        verify(metrics).seckillResult(SeckillMetrics.RESULT_NOT_START);
    }

    @Test
    @DisplayName("Lua 返回 5（已结束）")
    void ended() {
        stubLua(5);

        Result result = service.seckillVoucher(VOUCHER_ID);

        assertFalse(result.getSuccess());
        assertEquals("活动已结束", result.getErrorMsg());
        verify(metrics).seckillResult(SeckillMetrics.RESULT_ENDED);
    }

    @Test
    @DisplayName("Lua 返回 null（脚本执行异常）：按系统繁忙处理，不能当成成功")
    void nullResultTreatedAsError() {
        doReturn(null).when(stringRedisTemplate)
                .execute(any(RedisScript.class), ArgumentMatchers.<String>anyList(), any(), any());

        Result result = service.seckillVoucher(VOUCHER_ID);

        assertFalse(result.getSuccess());
        assertEquals("系统繁忙，请稍后重试", result.getErrorMsg());
        verify(metrics).seckillResult(SeckillMetrics.RESULT_ERROR);
    }

    @Test
    @DisplayName("预扣成功 + 消息投递成功：返回订单号，且消息里的三个字段不能串（orderId/userId/voucherId）")
    void successReturnsOrderId() {
        stubLua(0);
        when(orderProducer.send(any(OrderMessage.class))).thenReturn(true);

        Result result = service.seckillVoucher(VOUCHER_ID);

        assertTrue(result.getSuccess());
        assertEquals(ORDER_ID, result.getData());
        // 用 ArgumentCaptor 校验投递内容，而不是只校验"调用过一次"——
        // userId 和 voucherId 传反了接口照样返回成功，只有落库时才会发现订单挂错人
        org.mockito.ArgumentCaptor<OrderMessage> captor =
                org.mockito.ArgumentCaptor.forClass(OrderMessage.class);
        verify(orderProducer).send(captor.capture());
        assertEquals(ORDER_ID, captor.getValue().getOrderId());
        assertEquals(USER_ID, captor.getValue().getUserId());
        assertEquals(VOUCHER_ID, captor.getValue().getVoucherId());
        verify(metrics).seckillResult(SeckillMetrics.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("预扣成功但投递失败：必须返回失败，不能给用户一个查不到的单号")
    void sendFailureReturnsError() {
        stubLua(0);
        when(orderProducer.send(any(OrderMessage.class))).thenReturn(false);

        Result result = service.seckillVoucher(VOUCHER_ID);

        assertFalse(result.getSuccess());
        assertEquals("系统繁忙，请稍后重试", result.getErrorMsg());
        verify(metrics).seckillResult(SeckillMetrics.RESULT_ERROR);
    }

    // ==========================================================
    // 结果查询：这里的每个用例对应一个"前端该怎么显示"的分支
    // ==========================================================

    /** getById 来自 MyBatis-Plus 的 ServiceImpl，这里用 spy 把它短路掉，不连数据库 */
    private VoucherOrderServiceImpl spyWithOrder(VoucherOrder order) {
        VoucherOrderServiceImpl spied = spy(service);
        doReturn(order).when(spied).getById(anyLong());
        return spied;
    }

    private SeckillResultDTO data(Result result) {
        return (SeckillResultDTO) result.getData();
    }

    /**
     * 造一个"刚刚生成"的真实雪花 ID。
     *
     * 查询接口会用订单号反解生成时间来判断等了多久，所以这里不能用常量：
     * 随便写的数字（比如 1234567890123）解出来是 2024 年初，会被判成超时。
     */
    private static long freshOrderId() {
        return new SnowflakeIdWorker().nextId();
    }

    /** 把真实雪花 ID 往前拨 deltaMs 毫秒（时间戳在 22 位以上，直接减即可） */
    private static long orderIdAgedBy(long id, long deltaMs) {
        return id - (deltaMs << 22);
    }

    @Test
    @DisplayName("查询：订单已落库 -> CREATED，且不应再去问 Redis")
    void queryCreated() {
        VoucherOrder order = new VoucherOrder()
                .setId(ORDER_ID).setUserId(USER_ID).setVoucherId(VOUCHER_ID).setStatus(1);

        Result result = spyWithOrder(order).querySeckillResult(ORDER_ID);

        assertTrue(result.getSuccess());
        assertEquals(SeckillResultDTO.STATE_CREATED, data(result).getState());
        assertEquals(1, data(result).getOrderStatus());
        // 数据库是唯一真相，落库之后不需要再查回滚标记
        verify(stringRedisTemplate, never()).hasKey(anyString());
        verify(metrics).seckillResultQuery(SeckillResultDTO.STATE_CREATED);
    }

    @Test
    @DisplayName("查询：订单被超时关单 -> CLOSED，不能和'没抢到'混成一个状态")
    void queryClosed() {
        VoucherOrder order = new VoucherOrder()
                .setId(ORDER_ID).setUserId(USER_ID).setVoucherId(VOUCHER_ID).setStatus(4);

        Result result = spyWithOrder(order).querySeckillResult(ORDER_ID);

        assertTrue(result.getSuccess());
        assertEquals(SeckillResultDTO.STATE_CLOSED, data(result).getState());
        assertEquals("订单已超时取消", data(result).getMsg());
    }

    @Test
    @DisplayName("查询：不是自己的订单 -> 拒绝。订单号猜不到不等于有权限")
    void queryForbidden() {
        VoucherOrder order = new VoucherOrder()
                .setId(ORDER_ID).setUserId(999L).setVoucherId(VOUCHER_ID).setStatus(1);

        Result result = spyWithOrder(order).querySeckillResult(ORDER_ID);

        assertFalse(result.getSuccess());
        assertEquals("无权查看该订单", result.getErrorMsg());
        verify(metrics).seckillResultQuery("forbidden");
    }

    @Test
    @DisplayName("查询：DB 无单、Redis 无回滚标记、订单刚生成 -> PENDING，绝不能报失败")
    void queryPending() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        Result result = spyWithOrder(null).querySeckillResult(freshOrderId());

        assertTrue(result.getSuccess());
        assertEquals(SeckillResultDTO.STATE_PENDING, data(result).getState());
        assertTrue(data(result).getRetryAfterMs() > 0, "PENDING 必须给前端一个轮询间隔");
    }

    @Test
    @DisplayName("查询：DB 无单、无回滚标记，但订单号已生成 2 分钟 -> TIMEOUT，不能永远 PENDING")
    void queryTimeoutWhenMessageLost() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        long staleId = orderIdAgedBy(freshOrderId(), 120_000L);

        Result result = spyWithOrder(null).querySeckillResult(staleId);

        assertTrue(result.getSuccess());
        assertEquals(SeckillResultDTO.STATE_TIMEOUT, data(result).getState());
        verify(metrics).seckillResultQuery(SeckillResultDTO.STATE_TIMEOUT);
    }

    @Test
    @DisplayName("查询：超时判定要让位给回滚标记——有回滚标记就是 FAILED，不是 TIMEOUT")
    void rolledBackBeatsTimeout() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);
        long staleId = orderIdAgedBy(freshOrderId(), 120_000L);

        Result result = spyWithOrder(null).querySeckillResult(staleId);

        assertEquals(SeckillResultDTO.STATE_FAILED, data(result).getState());
    }

    @Test
    @DisplayName("查询：DB 无单但存在回滚标记 -> FAILED，这才是真的没抢到")
    void queryFailed() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);

        Result result = spyWithOrder(null).querySeckillResult(ORDER_ID);

        assertTrue(result.getSuccess());
        assertEquals(SeckillResultDTO.STATE_FAILED, data(result).getState());
    }

    @Test
    @DisplayName("查询：Redis 抖动时按 PENDING 返回，不能把'不知道'说成'失败了'")
    void queryWhenRedisDownReturnsPending() {
        when(stringRedisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("redis down"));

        Result result = spyWithOrder(null).querySeckillResult(freshOrderId());

        assertTrue(result.getSuccess());
        assertEquals(SeckillResultDTO.STATE_PENDING, data(result).getState());
    }
}
