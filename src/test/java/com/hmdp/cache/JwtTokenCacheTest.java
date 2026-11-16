package com.hmdp.cache;

import com.hmdp.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * JWT 本地缓存的单元测试。
 *
 * 这块最危险的地方不是"能不能命中"，而是**命中得比 token 活得久**：
 * 一旦缓存 TTL 超过 token 剩余有效期，就等于给过期凭证续命，
 * 所以下面专门用一个用例把"取两者较小值"钉死。
 */
class JwtTokenCacheTest {

    private JwtTokenCache cache;
    private UserDTO user;

    @BeforeEach
    void setUp() {
        cache = new JwtTokenCache();
        ReflectionTestUtils.setField(cache, "enabled", true);
        ReflectionTestUtils.setField(cache, "seconds", 30);

        user = new UserDTO();
        user.setId(7L);
        user.setNickName("cache-user");
    }

    @Test
    @DisplayName("写入后能命中，且返回的是同一个用户对象")
    void putThenHit() {
        String token = "token-abc";
        cache.put(token, user, 60_000L);

        UserDTO cached = cache.get(token);
        assertNotNull(cached);
        assertEquals(7L, cached.getId());
        assertEquals("cache-user", cached.getNickName());
        assertEquals(1L, cache.getHit());
        assertEquals(0L, cache.getMiss());
    }

    @Test
    @DisplayName("未写入的 token 必须返回 null，让调用方走完整校验")
    void missReturnsNull() {
        assertNull(cache.get("never-seen"));
        assertEquals(1L, cache.getMiss());
    }

    @Test
    @DisplayName("token 剩余有效期比配置 TTL 短时，必须按剩余有效期过期（否则等于给过期 token 续命）")
    void ttlTakesMinOfTokenRemain() throws InterruptedException {
        String token = "token-short-lived";
        // token 只剩 60ms，配置 TTL 是 30s —— 必须按 60ms 算
        cache.put(token, user, 60L);
        assertNotNull(cache.get(token), "刚写入应当命中");

        Thread.sleep(120L);
        assertNull(cache.get(token), "token 已过期，缓存必须同时失效");
    }

    @Test
    @DisplayName("登出后本机缓存立即失效")
    void evict() {
        String token = "token-logout";
        cache.put(token, user, 60_000L);
        assertNotNull(cache.get(token));

        cache.evict(token);
        assertNull(cache.get(token));
    }

    @Test
    @DisplayName("token 剩余有效期 <= 0 时不写入，避免缓存一个已失效的凭证")
    void notPutWhenExpired() {
        cache.put("token-expired", user, 0L);
        assertNull(cache.get("token-expired"));
    }

    @Test
    @DisplayName("开关关闭时必须完全不命中，退化为每请求验签 + 查黑名单")
    void disabled() {
        cache.put("token-x", user, 60_000L);
        ReflectionTestUtils.setField(cache, "enabled", false);

        assertNull(cache.get("token-x"), "开关关闭后不允许再返回缓存结果");
    }
}
