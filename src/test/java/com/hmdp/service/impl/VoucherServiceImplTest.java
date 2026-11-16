package com.hmdp.service.impl;

import com.hmdp.entity.Voucher;
import com.hmdp.exception.ParamException;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.service.ISeckillVoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 券写入口的校验。
 *
 * 覆盖的是实测出来的两类 500：
 *   title 300 字 -> Data too long for column 'title'
 *   payValue -1  -> Out of range value for column 'pay_value'（列是 unsigned bigint）
 * 以及秒杀券独有的两个静默坑：
 *   不传 beginTime -> toEpochMilli 抛 NPE
 *   结束早于开始   -> 券建成功但活动窗口为负，谁都抢不到，还不报错
 */
@ExtendWith(MockitoExtension.class)
class VoucherServiceImplTest {

    @Mock
    private ISeckillVoucherService seckillVoucherService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private VoucherMapper voucherMapper;

    private VoucherServiceImpl voucherService;

    @BeforeEach
    void setUp() {
        voucherService = new VoucherServiceImpl(seckillVoucherService, stringRedisTemplate);
        ReflectionTestUtils.setField(voucherService, "baseMapper", voucherMapper);
    }

    @Test
    @DisplayName("保存正常券：校验放行，落到 mapper")
    void saveValidVoucher() {
        when(voucherMapper.insert(any(Voucher.class))).thenReturn(1);

        boolean saved = voucherService.save(validVoucher());

        assertTrue(saved);
        verify(voucherMapper).insert(any(Voucher.class));
    }

    @Test
    @DisplayName("券标题超 255 字：拒绝，且一条 SQL 都不发")
    void saveRejectsTooLongTitle() {
        Voucher voucher = validVoucher();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            sb.append('券');
        }
        voucher.setTitle(sb.toString());

        ParamException e = assertThrows(ParamException.class, () -> voucherService.save(voucher));

        assertEquals("券标题最多 255 个字符", e.getMessage());
        verify(voucherMapper, never()).insert(any(Voucher.class));
    }

    @Test
    @DisplayName("支付金额为负：拒绝（列是 unsigned bigint）")
    void saveRejectsNegativePayValue() {
        Voucher voucher = validVoucher();
        voucher.setPayValue(-1L);

        ParamException e = assertThrows(ParamException.class, () -> voucherService.save(voucher));

        assertEquals("支付金额不能为负数", e.getMessage());
        verify(voucherMapper, never()).insert(any(Voucher.class));
    }

    @Test
    @DisplayName("秒杀窗口：结束时间必须晚于开始时间")
    void seckillWindowRejectsReversedRange() {
        Voucher voucher = validVoucher();
        voucher.setStock(10);
        voucher.setBeginTime(LocalDateTime.of(2026, 11, 8, 22, 0));
        voucher.setEndTime(LocalDateTime.of(2026, 11, 8, 21, 0));

        ParamException e = assertThrows(ParamException.class,
                () -> VoucherServiceImpl.validateSeckillWindow(voucher));

        assertEquals("活动结束时间必须晚于开始时间", e.getMessage());
    }

    @Test
    @DisplayName("秒杀窗口：缺开始时间给的是字段名，不是 NPE")
    void seckillWindowRejectsMissingBeginTime() {
        Voucher voucher = validVoucher();
        voucher.setStock(10);
        voucher.setEndTime(LocalDateTime.of(2026, 11, 8, 22, 0));

        ParamException e = assertThrows(ParamException.class,
                () -> VoucherServiceImpl.validateSeckillWindow(voucher));

        assertEquals("活动开始时间不能为空", e.getMessage());
    }

    @Test
    @DisplayName("秒杀窗口：库存为负拒绝，正常窗口放行")
    void seckillWindowChecksStock() {
        Voucher voucher = validVoucher();
        voucher.setStock(-1);
        voucher.setBeginTime(LocalDateTime.of(2026, 11, 8, 20, 0));
        voucher.setEndTime(LocalDateTime.of(2026, 11, 8, 22, 0));

        assertEquals("秒杀库存不能为负数",
                assertThrows(ParamException.class,
                        () -> VoucherServiceImpl.validateSeckillWindow(voucher)).getMessage());

        voucher.setStock(0);
        assertDoesNotThrow(() -> VoucherServiceImpl.validateSeckillWindow(voucher));
    }

    private static Voucher validVoucher() {
        Voucher voucher = new Voucher();
        voucher.setTitle("测试券");
        voucher.setPayValue(1L);
        voucher.setActualValue(10L);
        return voucher;
    }
}
