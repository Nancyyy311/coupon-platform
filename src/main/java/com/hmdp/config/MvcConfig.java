package com.hmdp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 配置。
 *
 * 改造说明：这里原来注册两个拦截器——
 *   RefreshTokenInterceptor（order=0）：每次请求从 header 取 token，查 Redis 会话并续期；
 *   LoginInterceptor（order=1）：判断 UserHolder 是否为空，为空返回 401。
 *
 * 换成 JWT 之后，这两件事分别由 JwtAuthenticationFilter（解析 token）和
 * SecurityConfig（白名单 + anyRequest().authenticated()）接管，所以这里不再注册任何拦截器。
 *
 * 两个拦截器类已经删除（连同 ILock / SimpleRedisLock / RedisIdWorker / PasswordEncoder
 * 这几个原版遗留类）。留着的代价不是"多几个文件"，而是面试官会问
 * "你的项目里为什么有两套鉴权、两套 ID 生成、一套没人用的锁实现"——
 * 答不上来就变成"代码是从教程抄的"。
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
}
