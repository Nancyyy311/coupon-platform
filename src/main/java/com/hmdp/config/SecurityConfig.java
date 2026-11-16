package com.hmdp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.cache.JwtTokenCache;
import com.hmdp.dto.Result;
import com.hmdp.enums.Role;
import com.hmdp.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * 安全配置：无状态 JWT 认证。
 *
 * 改造前后对比：
 *   改造前：MvcConfig 注册两个拦截器——RefreshTokenInterceptor 负责"每次请求查 Redis 续期"，
 *          LoginInterceptor 负责"没登录就返回 401"。鉴权逻辑散在拦截器 + 各处 if 判断里。
 *   改造后：白名单统一在 Security 里声明，其余请求必须携带有效 JWT；
 *          未登录由 AuthenticationEntryPoint 统一返回 401 JSON，业务代码不再关心"有没有登录"。
 *
 * 为什么这里还是 WebSecurityConfigurerAdapter（这是个会被追问的点，要能答）：
 *   我确实试过换成官方推荐的 SecurityFilterChain Bean 写法，结果启动直接失败：
 *     NoSuchBeanDefinitionException: No qualifying bean of type 'HttpSecurity'
 *   查源码确认原因是版本：SecurityFilterChain 作为 Bean + 注入 HttpSecurity 的写法
 *   依赖 Security 5.4 引入的 HttpSecurityConfiguration；而这个项目是 Boot 2.3.12，
 *   带的是 Security 5.3，HttpSecurity 还不是 Bean，只能由 adapter 的 configure() 拿到。
 *   WebSecurityConfigurerAdapter 是在 Security 5.7 才标记 @Deprecated 的，
 *   在 5.3 里它就是当时的标准写法，不算"用了过时 API"。
 *
 *   那为什么不干脆把 Boot 升到 2.7 / 3.x？
 *   算过账：升 2.7 要动 MyBatis-Plus（3.4.3 对 2.7 不保证兼容）、spring.redis 前缀改
 *   spring.data.redis、Security 的 antMatchers 系列迁移；升 3.x 还要 javax→jakarta，
 *   全项目 import 都要改。而这些改动对"秒杀链路的正确性和性能"没有任何提升，
 *   却会把回归面扩大好几倍。当前阶段收益更大的事是链路本身，不是框架版本。
 *   面试被问就照实说这个判断，比硬升上去然后一堆兼容问题更站得住。
 *
 * 一个容易踩的坑（面试常问）：
 *   JwtAuthenticationFilter 用 new 而不是声明成 @Bean。
 *   它是 OncePerRequestFilter，如果注册成 Spring Bean，Boot 会把它自动挂到 Servlet 过滤器链上，
 *   加上 Security 链里手动加的那一次，同一个请求会执行两遍（token 验两次、缓存写两次）。
 *   所以要控制"只加一次"，就不要让它进容器。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * 白名单必须**按 HTTP 方法拆开**，不能按路径前缀一把放。
     *
     * 这是改造里一个真实的安全漏洞，而且是"看起来没问题"的那种：
     *   原来写的是 `antMatchers("/shop/**", "/voucher/**", "/upload/**").permitAll()`，
     *   本意是"游客能看店铺、能看券"，但 `/shop/**` 同时覆盖了 `POST /shop`（新建店铺）
     *   和 `PUT /shop`（改店铺），`/voucher/**` 覆盖了 `POST /voucher/seckill`（发秒杀券），
     *   `/upload/**` 覆盖了上传和删除图片。
     *   实测：不带任何 token，能发券、能改任意店铺、能上传 .html、还能用 `../` 删掉
     *   imgs 上一级的文件。四个动作全部 200。
     *
     * 所以这里按方法拆：游客只能 GET；写操作一律要求认证。
     * 上传/删除不进白名单，登录后才可用。
     */
    private static final String[] PUBLIC_GET = {
            "/shop/**",
            "/voucher/**",
            "/shop-type/**",
            "/blog/hot"
    };

    /** 唯一免登录的写接口：发验证码、登录本身 */
    private static final String[] PUBLIC_POST = {
            "/user/code",
            "/user/login"
    };

    /** 与请求方法无关的公共资源：/error 不放开会把真实错误盖成 401，/actuator 用于压测抓指标 */
    private static final String[] PUBLIC_ANY = {
            "/error",
            "/actuator/**",
            "/favicon.ico"
    };

    /**
     * 只有商家能做的写操作。
     *
     * 这一层是补"匿名写接口"之后剩下的另一半：
     *   上一轮按 HTTP 方法把白名单拆开，匿名已经建不了店、发不了券（401）；
     *   但**任何登录用户**仍然可以——因为当时项目里只有"登录/未登录"，没有"商家"这个概念，
     *   `tb_user` 连角色列都没有。实测用普通账号 `POST /voucher` 能建成一张券。
     *
     * 用 Security 的 {@code hasRole} 而不是自定义注解 + 拦截器：
     *   hasRole 的判定点在过滤器链里，先于 Controller 参数绑定执行，
     *   越权的请求连 JSON 都不会被反序列化；而且拒绝路径统一由 AccessDeniedHandler 出，
     *   不会出现"这个接口忘了加注解"的漏网——漏了注解的接口不会报错，只会静默放行，
     *   而这里的配置是"默认全拦，显式放行"，写漏了是 403 而不是 200。
     */
    private static final String[] MERCHANT_POST = {"/shop", "/voucher", "/voucher/seckill"};
    private static final String[] MERCHANT_PUT = {"/shop"};

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final JwtTokenCache jwtTokenCache;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 1. 关 CSRF：纯 token 认证没有 cookie 会话可利用，开着会让所有 POST 被 403
        http.csrf(csrf -> csrf.disable());

        // 2. 完全无状态：不创建 HttpSession，也不从 Session 取认证信息
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 3. 白名单按方法放行；商家写接口要求 MERCHANT；其余一律要求认证
        //    顺序有意义：更具体的规则必须写在 anyRequest 之前，否则永远匹配不到。
        http.authorizeRequests(reg -> reg
                .antMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                .antMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                .antMatchers(PUBLIC_ANY).permitAll()
                .antMatchers(HttpMethod.POST, MERCHANT_POST).hasRole(Role.MERCHANT.name())
                .antMatchers(HttpMethod.PUT, MERCHANT_PUT).hasRole(Role.MERCHANT.name())
                .anyRequest().authenticated());

        // 4. 未认证（401）与已认证但无权（403）分开返回 JSON，不能让它跳默认登录页
        //    为什么 403 不像参数错误那样给 200 + success=false：
        //    参数错误是"你改了参数还能成功"，权限不足是"这个身份做什么都不行"，
        //    属于能不能进门的差别，与 401 同族，必须留在 4xx 里，否则越权尝试会被监控算成正常流量。
        http.exceptionHandling(eh -> eh
                .authenticationEntryPoint(unauthorizedEntryPoint())
                .accessDeniedHandler(forbiddenHandler()));

        // 5. JWT 过滤器插在用户名密码认证过滤器之前
        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtUtil, stringRedisTemplate, jwtTokenCache),
                UsernamePasswordAuthenticationFilter.class);

        // 6. 关掉自带登录页 / Basic / 默认登出，避免和自定义接口冲突
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());
        http.logout(logout -> logout.disable());
    }

    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, ex) -> writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录");
    }

    /**
     * 已登录但角色不够（比如普通用户发券）。
     *
     * 文案里明确写"需要商家权限"，不要只说"无权限"：
     * 前端接入时能一眼分清是没登录（401）还是登录了但身份不对（403），
     * 排查时也不用去翻代码确认这个接口到底要什么角色。
     */
    private AccessDeniedHandler forbiddenHandler() {
        return (request, response, ex) -> writeJson(response, HttpServletResponse.SC_FORBIDDEN, "无权限执行该操作（需要商家身份）");
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(message)));
    }
}
