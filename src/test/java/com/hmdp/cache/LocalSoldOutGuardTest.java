package com.hmdp.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地售罄标识的单元测试。
 *
 * 这个类只负责"挡流量"，不承担任何正确性判断，所以测试重点也不是
 * "它会不会误判"，而是三件事：
 *   1) 标记/清除能正确翻转状态
 *   2) 按 voucherId 隔离，不能把 A 券的售罄安到 B 券头上
 *   3) 开关关闭时永远返回 false（否则关掉开关也拦流量，回退手段就是假的）
 */
class LocalSoldOutGuardTest {

    private LocalSoldOutGuard guard;

    @BeforeEach
    void setUp() {
        guard = new LocalSoldOutGuard();
        ReflectionTestUtils.setField(guard, "enabled", true);
    }

    @Test
    @DisplayName("标记后判售罄，清除后恢复可抢")
    void markAndClear() {
        Long voucherId = 7L;

        assertFalse(guard.isSoldOut(voucherId), "初始状态不应是售罄");
        guard.markSoldOut(voucherId);
        assertTrue(guard.isSoldOut(voucherId));

        // 退单回补库存时必须能清掉，否则用户要等 30 秒才能再抢
        guard.clear(voucherId);
        assertFalse(guard.isSoldOut(voucherId));
    }

    @Test
    @DisplayName("按券隔离：A 券售罄不影响 B 券")
    void isolatedByVoucher() {
        guard.markSoldOut(7L);

        assertTrue(guard.isSoldOut(7L));
        assertFalse(guard.isSoldOut(8L));
    }

    @Test
    @DisplayName("开关关闭时永远不拦，保证回退开关是真的能回退")
    void disabledNeverBlocks() {
        guard.markSoldOut(7L);
        ReflectionTestUtils.setField(guard, "enabled", false);

        assertFalse(guard.isSoldOut(7L));
    }

    @Test
    @DisplayName("开关关闭时标记动作也不生效，避免开关重开后出现「幽灵售罄」")
    void markIgnoredWhenDisabled() {
        ReflectionTestUtils.setField(guard, "enabled", false);
        guard.markSoldOut(7L);

        ReflectionTestUtils.setField(guard, "enabled", true);
        assertFalse(guard.isSoldOut(7L), "关闭期间不应留下标记");
    }
}
