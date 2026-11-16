package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT 签发与校验的单元测试（不启动 Spring 上下文，毫秒级）。
 *
 * 覆盖的是"改造后新增、且一旦出错就是安全漏洞"的几处：
 * 签名不可伪造、过期必须失效、登出黑名单依赖的 jti 必须每次不同。
 */
class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-bytes-long";

    private JwtUtil jwtUtil;

    private UserDTO user;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "ttlMinutes", 60L);

        user = new UserDTO();
        user.setId(42L);
        user.setNickName("tester");
        user.setIcon("/imgs/icons/1.png");
    }

    @Test
    @DisplayName("签发的 token 是标准三段式，且能解析回原用户")
    void generateThenParse() {
        String token = jwtUtil.generate(user);

        // header.payload.signature —— jwt.io 能直接解码，不能是自定义格式
        assertEquals(3, token.split("\\.").length);

        Claims claims = jwtUtil.parse(token);
        UserDTO parsed = jwtUtil.toUser(claims);

        assertEquals(42L, parsed.getId());
        assertEquals("tester", parsed.getNickName());
        assertEquals("/imgs/icons/1.png", parsed.getIcon());
    }

    @Test
    @DisplayName("每次登录的 jti 都不同，否则登出黑名单会把别人的 token 一起拉黑")
    void jtiIsUnique() {
        Claims c1 = jwtUtil.parse(jwtUtil.generate(user));
        Claims c2 = jwtUtil.parse(jwtUtil.generate(user));

        assertNotNull(c1.getId());
        assertNotNull(c2.getId());
        assertTrue(!c1.getId().equals(c2.getId()), "两次签发的 jti 必须不同");
    }

    @Test
    @DisplayName("篡改签名后必须验签失败")
    void tamperedTokenRejected() {
        String token = jwtUtil.generate(user);
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThrows(JwtException.class, () -> jwtUtil.parse(tampered));
    }

    @Test
    @DisplayName("换成别的密钥签发的 token 必须验签失败（防止跨环境/跨服务串用）")
    void foreignSecretRejected() {
        JwtUtil other = new JwtUtil();
        ReflectionTestUtils.setField(other, "secret", "another-secret-key-that-is-long-enough-32b");
        ReflectionTestUtils.setField(other, "ttlMinutes", 60L);

        String foreign = other.generate(user);
        assertThrows(JwtException.class, () -> jwtUtil.parse(foreign));
    }

    @Test
    @DisplayName("已过期的 token 必须被拒绝，且异常类型是 Expired（便于上层区分）")
    void expiredTokenRejected() {
        JwtUtil shortLived = new JwtUtil();
        ReflectionTestUtils.setField(shortLived, "secret", SECRET);
        // ttl 为负一分钟 → 签发即过期
        ReflectionTestUtils.setField(shortLived, "ttlMinutes", -1L);

        String token = shortLived.generate(user);
        assertThrows(ExpiredJwtException.class, () -> shortLived.parse(token));
    }

    @Test
    @DisplayName("剩余有效期用于设置黑名单 TTL，必须为正且不超过 token 总寿命")
    void remainMillis() {
        Claims claims = jwtUtil.parse(jwtUtil.generate(user));
        long remain = jwtUtil.remainMillis(claims);

        assertTrue(remain > 0, "刚签发的 token 剩余有效期必须为正");
        assertTrue(remain <= 60 * 60_000L, "剩余有效期不能超过配置的 ttl");
    }

    @Test
    @DisplayName("角色随 token 签发，解析回来必须是归一化后的值")
    void roleRoundTrip() {
        user.setRole(" merchant "); // 模拟库里存了带空格的小写值

        UserDTO parsed = jwtUtil.toUser(jwtUtil.parse(jwtUtil.generate(user)));

        // 写进 payload 的必须已经归一化，否则端点比对 "ROLE_MERCHANT" 会漏
        assertEquals("MERCHANT", parsed.getRole());
    }

    @Test
    @DisplayName("本次改造前签发的旧 token 没有 role 字段，必须降级成 USER")
    void legacyTokenWithoutRoleDowngrades() {
        // 用不含 role 的老式 payload 造一个 token：升级上线时，
        // 存量用户的 token 都长这样，不能让他们直接 500，更不能默认给商家权限
        String legacy = io.jsonwebtoken.Jwts.builder()
                .setClaims(new java.util.HashMap<String, Object>())
                .claim("userId", 42L)
                .claim("nickName", "tester")
                .setId("legacy-jti")
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60_000L))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        UserDTO parsed = jwtUtil.toUser(jwtUtil.parse(legacy));

        assertEquals(42L, parsed.getId());
        assertEquals("USER", parsed.getRole(), "旧 token 必须按最小权限处理");
    }
}
