package com.hmdp.config;

import com.hmdp.cache.JwtTokenCache;
import com.hmdp.dto.UserDTO;
import com.hmdp.enums.Role;
import com.hmdp.utils.JwtUtil;
import com.hmdp.utils.UserHolder;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * 从请求头解析 JWT，把身份放进 SecurityContext（顺带兼容原来的 UserHolder）。
 *
 * 为什么还保留 UserHolder：
 *   业务代码里大量使用 UserHolder.getUser()，为了不把这几十处全改一遍，
 *   在这里把解析结果同时写进 UserHolder 和 SecurityContext，
 *   两者在同一个请求线程内保持一致，请求结束统一清理。
 *
 * 这里不抛异常：token 无效就当作"未登录"放行，
 * 后面由 Security 的 anyRequest().authenticated() 拦下并返回 401 JSON。
 *
 * 性能：先查本机 JwtTokenCache，命中则跳过签名计算与黑名单查询；
 * 未命中才走"验签 → 查 jti 黑名单 → 回填"。
 * 关掉 app.jwt.local-cache-enabled 即回到每请求一次 Redis 的原始行为，便于压测对比。
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 与 org.springframework.security 的默认头区分开，沿用原版前端约定的 authorization */
    private static final String HEADER = "authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenCache jwtTokenCache;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token)) {
                // 快路径：本机缓存命中，既不算签名也不查 Redis。
                // 缓存在登出时会通过广播清除，所以"登出即时生效"仍然成立。
                UserDTO cached = jwtTokenCache.get(token);
                if (cached != null) {
                    saveUser(cached);
                } else {
                    Claims claims = jwtUtil.parse(token);
                    // 登出后的 token 仍在有效期内，用 jti 黑名单挡掉
                    if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + claims.getId()))) {
                        UserDTO user = jwtUtil.toUser(claims);
                        saveUser(user);
                        jwtTokenCache.put(token, user, jwtUtil.remainMillis(claims));
                    }
                }
            }
        } catch (Exception e) {
            // 签名不对 / 过期 / 格式错误，一律按未登录处理，不阻断请求链路
            log.debug("[JWT] token 校验未通过: {}", e.getMessage());
        }

        try {
            chain.doFilter(request, response);
        } finally {
            UserHolder.removeUser();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 身份同时写入 UserHolder（业务代码用）和 SecurityContext（Security 用）。
     *
     * 权限列表不能是空集合：Security 的 {@code hasRole("MERCHANT")} 靠 authorities 判定，
     * 空集合意味着"已登录但什么角色都没有"，所有商家接口都会 403。
     * 这里只放一个角色权限，不做"角色叠加"——本项目两级角色是互斥的。
     */
    private void saveUser(UserDTO user) {
        UserHolder.saveUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user, null,
                        Collections.singletonList(
                                new SimpleGrantedAuthority(Role.of(user.getRole()).authority()))));
    }

    /**
     * 同时兼容 "Bearer xxx" 和裸 token：前者是标准写法，后者是原版前端的传法。
     */
    private String resolveToken(HttpServletRequest request) {
        String raw = request.getHeader(HEADER);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.startsWith(BEARER_PREFIX) ? raw.substring(BEARER_PREFIX.length()).trim() : raw.trim();
    }
}
