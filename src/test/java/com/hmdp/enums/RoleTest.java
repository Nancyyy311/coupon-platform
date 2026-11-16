package com.hmdp.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 角色解析的单元测试。
 *
 * 这个类很小，但它守的是"权限系统的入口"：
 * 解析错一次，要么商家做不了事（全 403），要么普通用户变成商家（越权）。
 * 所以四个方向都要覆盖：空、大小写、脏值、权限前缀。
 */
class RoleTest {

    @Test
    @DisplayName("空角色按普通用户处理，不能抛异常")
    void nullBecomesUser() {
        // 库里有默认值，但旧 token 不带 role 字段，toUser 会传 null 进来。
        // 这里如果用 valueOf(null) 就是 NPE → 每次带旧 token 的请求都 500。
        assertEquals(Role.USER, Role.of(null));
        assertEquals("USER", Role.normalize(null));
    }

    @Test
    @DisplayName("大小写和空格都要认（数据库是 varchar，不保证写入规范）")
    void caseAndSpaceInsensitive() {
        assertEquals(Role.MERCHANT, Role.of("MERCHANT"));
        assertEquals(Role.MERCHANT, Role.of("merchant"));
        assertEquals(Role.MERCHANT, Role.of("  Merchant  "));
        assertEquals("MERCHANT", Role.normalize(" merchant "));
    }

    @Test
    @DisplayName("不认识的脏值降级成 USER，不是放行也不是报错")
    void unknownDowngradesToUser() {
        // 三种真实会出现的脏值：手工改库写错、将来新增角色但旧代码不认识、空串
        assertEquals(Role.USER, Role.of("ADMIN"));
        assertEquals(Role.USER, Role.of(""));
        assertEquals(Role.USER, Role.of("MERCHANT_X"));
        // 关键断言：不能因为"以 MERCHANT 开头"就放行
        assertNotEquals(Role.MERCHANT, Role.of("MERCHANT_X"));
    }

    @Test
    @DisplayName("authority 必须是 ROLE_ 前缀，否则 hasRole 永远不通过")
    void authorityHasPrefix() {
        // Spring Security 的 hasRole("MERCHANT") 内部比对的是 "ROLE_MERCHANT"。
        // 少了前缀的表现是"商家也 403"，而且日志里看不出原因，只能靠这条断言提前挡住。
        assertEquals("ROLE_MERCHANT", Role.MERCHANT.authority());
        assertEquals("ROLE_USER", Role.USER.authority());
    }
}
