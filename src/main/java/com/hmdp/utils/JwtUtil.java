package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 签发与校验。
 *
 * 为什么换成 JWT（改造前后对比，面试可以直接讲）：
 *   改造前：登录时生成随机串，把整个 UserDTO 塞进 Redis Hash，每次请求再查回来。
 *          token 本身没有任何信息量，离开 Redis 就失效——所以 Redis 一旦抖动，
 *          全站登录态一起掉；而且这本质上是"用 Redis 存会话"，不是无状态认证。
 *   改造后：身份信息由签名保证，服务端不保存会话。请求进来只做一次签名校验（纯 CPU 计算），
 *          不产生任何 Redis 读。JWT 里带上 userId/nickName/icon，
 *          连"取用户信息"的那次查询也省了。
 *
 * 已知代价（必须主动说，否则会被追问）：
 *   1. 无状态意味着无法"主动踢人"——所以登出走 jti 黑名单，见 UserController#logout。
 *   2. token 一旦签发，在过期前都有效，所以有效期不能太长（默认 1 天）。
 *   3. 用户昵称改了，旧 token 里还是旧昵称，直到重新登录。
 *
 * 密钥处理（0.9.1 → 0.11.5 升级后的写法）：
 *   用 Keys.hmacShaKeyFor(byte[]) 生成 SecretKey，由库自己校验长度。
 *   HS256 要求密钥 >= 256 bit（32 字节），密钥太短会直接抛 WeakKeyException，
 *   而不是像 0.9.1 那样"能用但一撞就破"。这就是升级的实际收益，不是换个版本号。
 *   SecretKey 是线程安全的不可变对象，懒加载一次后缓存复用，不要每请求重新构造。
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.ttl-minutes:1440}")
    private long ttlMinutes;

    /** 由配置里的字符串派生，首次使用时构造一次。volatile 保证多线程下的可见性 */
    private volatile SecretKey signingKey;

    /**
     * 解析器缓存。这里是本轮唯一一次"改完反而变慢"，值得说清楚：
     *
     * 第一版我把 Jwts.parserBuilder().build() 直接写在了 parse() 里，等于**每个请求重建一次解析器**。
     * 在 0.9.1 上这么写还能跑到 2.7 万 QPS，升到 0.11.5 之后掉到 2,600 —— 同一个接口、同一台机器，
     * 关掉本地缓存时 P50 从 1.5ms 涨到 23.8ms。
     *
     * 原因是 0.11.x 的 build() 会初始化 Jackson 的 Deserializer（这个版本把实现拆到了 jjwt-jackson，
     * 解析器构建时会做一次反序列化器的装配），属于"重量级对象"，成本在毫秒级；
     * 4 进程 × 16 并发下被放大成 10 倍 QPS 差距。JwtParser 官方保证线程安全，构建一次复用即可。
     *
     * 这条不是"背结论"，是被 A/B 打出来的：换依赖之后复跑基准，BEFORE 组掉了一个数量级，
     * 而只改缓存开关的 AFTER 组没变——差异只可能落在"每次请求都走的新代码"上。
     */
    private volatile JwtParser jwtParser;

    private SecretKey signingKey() {
        SecretKey k = signingKey;
        if (k == null) {
            k = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            signingKey = k;
        }
        return k;
    }

    /** 双重检查：并发首次请求时只构建一次，后续直接读 volatile 字段 */
    private JwtParser parser() {
        JwtParser p = jwtParser;
        if (p == null) {
            synchronized (this) {
                p = jwtParser;
                if (p == null) {
                    p = Jwts.parserBuilder()
                            .setSigningKey(signingKey())
                            .build();
                    jwtParser = p;
                }
            }
        }
        return p;
    }

    /**
     * 签发 token。jti 用于登出黑名单，每次登录都不同。
     *
     * role 一并写进 payload：写接口的鉴权（{@code hasRole("MERCHANT")}）只读 token，
     * 不查库、不查 Redis。代价是改角色要重新登录才生效，见 {@link com.hmdp.enums.Role}。
     */
    public String generate(UserDTO user) {
        Map<String, Object> claims = new HashMap<>(8);
        claims.put("userId", user.getId());
        claims.put("nickName", user.getNickName());
        claims.put("icon", user.getIcon());
        // 归一化后再写：库里是 'user'/'USER' 两种写法都能被 Role.of 收口，
        // 但 token 里必须只有一种，否则下游 hasRole 的字符串比对会漏
        claims.put("role", Role.normalize(user.getRole()));

        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ttlMinutes * 60_000L))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析并校验签名与有效期。签名错、被篡改、已过期都会抛异常，由调用方统一处理。
     */
    public Claims parse(String token) {
        return parser()
                .parseClaimsJws(token)
                .getBody();
    }

    public UserDTO toUser(Claims claims) {
        UserDTO user = new UserDTO();
        Object id = claims.get("userId");
        if (id != null) {
            user.setId(((Number) id).longValue());
        }
        user.setNickName((String) claims.get("nickName"));
        user.setIcon((String) claims.get("icon"));
        // 本次改造前签发的旧 token 没有 role 字段，Role.of(null) 会降级成 USER：
        // 老 token 继续能读、能发笔记，但拿不到商家权限。这是有意的"失败即最小权限"。
        user.setRole(Role.normalize((String) claims.get("role")));
        return user;
    }

    /**
     * token 剩余有效期（毫秒），用于设置黑名单 TTL，避免黑名单无限增长。
     */
    public long remainMillis(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration == null ? 0L : expiration.getTime() - System.currentTimeMillis();
    }
}
