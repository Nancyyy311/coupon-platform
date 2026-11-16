package com.hmdp.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hmdp.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JWT 认证结果的本地缓存（性能优化）。
 *
 * 为什么需要它（这是"无状态"叙事里最容易漏的一环）：
 * ------------------------------------------------------------------
 * 只做 JWT 验签确实不碰 Redis。但"登出即时生效"要靠 jti 黑名单，
 * 而黑名单在 Redis 里 —— 于是每个请求都会多一次 hasKey 往返。
 * 也就是说：**说好的零 Redis 读，实际每请求一次**，只是从"读会话"变成了"读黑名单"。
 *
 * 本类把"验签结果 + 未拉黑"这个结论在进程内缓存一小段时间：
 *   - 命中：零 HMAC 计算、零 Redis 往返，直接拿 UserDTO
 *   - 未命中：正常验签 + 查黑名单，然后回填
 *
 * 代价与处理（面试必须主动说）：
 *   本地缓存意味着"登出"这个动作不会立刻在别的实例生效。
 *   处理方式是复用店铺缓存那套广播：登出时删本机缓存 + pub/sub 通知其他实例，
 *   所以默认配置下登出仍然是即时的；广播丢失时最坏多容忍 local-cache-seconds（默认 30 秒）。
 *
 * 为什么不缓存到 token 自身过期：
 *   缓存里存了"已通过认证"的结论，一旦持有时间超过 token 有效期，
 *   就等于给过期 token 续了命。所以回填时取 min(token 剩余有效期, 配置 TTL)。
 */
@Slf4j
@Component
public class JwtTokenCache {

    /** 失效广播频道：登出时通知其他实例删掉本机缓存 */
    public static final String CHANNEL = "cache:invalidate:jwt";

    /** 一键开关：关掉后退化为"每请求都验签 + 查 Redis 黑名单"的原始行为 */
    @Value("${app.jwt.local-cache-enabled:true}")
    private boolean enabled;

    /** 本地缓存存活秒数。越短越安全（登出/封号生效越快），越长越省 Redis */
    @Value("${app.jwt.local-cache-seconds:30}")
    private int seconds;

    private final Cache<String, Entry> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(60, TimeUnit.SECONDS)   // 兜底清理，真实有效期见 Entry.expireAt
            .recordStats()
            .build();

    private final AtomicLong hit = new AtomicLong();
    private final AtomicLong miss = new AtomicLong();

    /** 命中返回用户，未命中返回 null（调用方走完整校验） */
    public UserDTO get(String token) {
        if (!enabled || token == null) {
            return null;
        }
        Entry entry = cache.getIfPresent(token);
        if (entry == null) {
            miss.incrementAndGet();
            return null;
        }
        if (entry.expireAt <= System.currentTimeMillis()) {
            // 已过逻辑有效期：主动清掉，避免 Caffeine 的兜底 TTL 还没到就放行过期凭证
            cache.invalidate(token);
            miss.incrementAndGet();
            return null;
        }
        hit.incrementAndGet();
        return entry.user;
    }

    /**
     * 回填。tokenRemainMillis 必须传 token 的真实剩余有效期，不能只传配置的 TTL。
     */
    public void put(String token, UserDTO user, long tokenRemainMillis) {
        if (!enabled || token == null || user == null || tokenRemainMillis <= 0) {
            return;
        }
        long ttl = Math.min(tokenRemainMillis, seconds * 1000L);
        cache.put(token, new Entry(user, System.currentTimeMillis() + ttl));
    }

    /** 登出时清掉本机这条（其他实例由 pub/sub 广播负责） */
    public void evict(String token) {
        if (token != null) {
            cache.invalidate(token);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getHit() {
        return hit.get();
    }

    public long getMiss() {
        return miss.get();
    }

    private static final class Entry {
        private final UserDTO user;
        private final long expireAt;

        private Entry(UserDTO user, long expireAt) {
            this.user = user;
            this.expireAt = expireAt;
        }
    }
}
