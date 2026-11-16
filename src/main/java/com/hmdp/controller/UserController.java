package com.hmdp.controller;


import cn.hutool.core.bean.BeanUtil;
import com.hmdp.cache.JwtTokenCache;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.JwtUtil;
import com.hmdp.utils.UserHolder;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /** 改造新增：解析 token 用于登出黑名单 */
    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 改造新增：认证结果本地缓存，登出时需要主动清除 */
    @Resource
    private JwtTokenCache jwtTokenCache;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        // 发送短信验证码并保存验证码
        return userService.sendCode(phone,session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        // 实现登录功能
        return userService.login(loginForm,session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request){
        // JWT 是无状态的，服务端没有"会话"可以删。
        // 做法：把本次 token 的 jti 写进黑名单，TTL 取 token 剩余有效期，
        // 这样 token 自然过期后黑名单条目也自动消失，不会无限堆积。
        String header = request.getHeader("authorization");
        if (header != null && header.trim().length() > 0) {
            String raw = header.startsWith("Bearer ") ? header.substring(7).trim() : header.trim();
            try {
                Claims claims = jwtUtil.parse(raw);
                long ttl = jwtUtil.remainMillis(claims);
                if (ttl > 0) {
                    stringRedisTemplate.opsForValue()
                            .set("jwt:blacklist:" + claims.getId(), "1", ttl, TimeUnit.MILLISECONDS);
                }
            } catch (Exception e) {
                // token 本身已失效或被篡改，登出按成功处理即可
                log.debug("[登出] token 校验未通过，视为已登出: {}", e.getMessage());
            }
            // 关键：黑名单只对"下一次校验"生效，而认证结果被本机缓存了。
            // 不主动清掉的话，登出后最多 local-cache-seconds 内还能用旧 token 访问。
            // 所以这里删本机 + 广播给其他实例，保证登出即时生效。
            jwtTokenCache.evict(raw);
            stringRedisTemplate.convertAndSend(JwtTokenCache.CHANNEL, raw);
        }
        return Result.ok();
    }

    @GetMapping("/me")
    public Result me(){
        // 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }

    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }

    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }


}
