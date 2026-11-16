package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 登录链路的边界测试。
 *
 * 这里测的三件事都是**实测复现过**的缺陷，不是假想：
 *   ① /user/code 匿名可达且没有任何频率限制，连打 30 次全部成功
 *   ② 验证码校验通过后没有失败次数上限，12 次错码之后码还在，可以继续穷举
 *   ③ 并发首登撞唯一索引时抛 DuplicateKeyException，被兜成"服务器异常"
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    private UserServiceImpl userService;

    private static final String PHONE = "13800138000";

    @BeforeEach
    void setUp() {
        userService = spy(new UserServiceImpl());
        ReflectionTestUtils.setField(userService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(userService, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    // ---------------- 发送验证码的频率限制 ----------------

    @Test
    @DisplayName("发码：60 秒内重复请求被拒，且不写验证码")
    void sendCodeThrottled() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        // SET NX 抢不到坑 = 刚发过
        when(valueOps.setIfAbsent(anyString(), anyString(), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        Result result = userService.sendCode(PHONE, null);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMsg().contains("60 秒"));
        // 关键：被节流时不能生成新验证码，否则"60 秒一次"只挡了响应没挡住副作用
        verify(valueOps, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("发码：超过 24 小时总量上限被拒")
    void sendCodeDailyLimit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        // 第 11 次
        when(valueOps.increment(anyString())).thenReturn(11L);

        Result result = userService.sendCode(PHONE, null);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMsg().contains("上限"));
        verify(valueOps, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("发码：首次请求成功，验证码写入 Redis 且 TTL 2 分钟，日计数带 24h TTL")
    void sendCodeOk() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(valueOps.increment(anyString())).thenReturn(1L);

        Result result = userService.sendCode(PHONE, null);

        assertTrue(result.getSuccess());
        verify(valueOps).set(eq("login:code:" + PHONE), anyString(), eq(2L), eq(TimeUnit.MINUTES));
        // 第一次计数必须带上 TTL，否则这个 key 会永久存在，用户第二天就再也发不出码
        verify(stringRedisTemplate).expire(eq("login:code:daily:" + PHONE), eq(1L), eq(TimeUnit.DAYS));
    }

    @Test
    @DisplayName("发码：手机号格式错误时连 Redis 都不碰")
    void sendCodeRejectsBadPhone() {
        Result result = userService.sendCode("12345", null);

        assertFalse(result.getSuccess());
        verifyNoInteractions(stringRedisTemplate);
    }

    // ---------------- 验证码校验的三种返回值 ----------------

    private void mockVerify(Long luaResult) {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any()))
                .thenReturn(luaResult);
    }

    private QueryChainWrapper<User> mockQueryChain() {
        @SuppressWarnings("unchecked")
        QueryChainWrapper<User> chain = mock(QueryChainWrapper.class);
        doReturn(chain).when(userService).query();
        when(chain.eq(anyString(), any())).thenReturn(chain);
        return chain;
    }

    private LoginFormDTO form(String code) {
        LoginFormDTO form = new LoginFormDTO();
        form.setPhone(PHONE);
        form.setCode(code);
        return form;
    }

    @Test
    @DisplayName("登录：验证码正确 -> 签发 token")
    void loginOk() {
        mockVerify(1L);
        QueryChainWrapper<User> chain = mockQueryChain();
        User user = new User();
        user.setId(1005L);
        user.setPhone(PHONE);
        user.setNickName("测试用户");
        when(chain.one()).thenReturn(user);
        when(jwtUtil.generate(any())).thenReturn("jwt-token");

        Result result = userService.login(form("123456"), null);

        assertTrue(result.getSuccess());
        assertEquals("jwt-token", result.getData());
        verify(jwtUtil).generate(any());
    }

    @Test
    @DisplayName("登录：错误次数超限 -> 提示重新获取，不再查库")
    void loginLocked() {
        mockVerify(2L);

        Result result = userService.login(form("000000"), null);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMsg().contains("次数过多"));
        // 码已经被脚本烧掉，这次请求不该再产生任何数据库访问
        verify(userMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("登录：验证码错误 -> 普通错误提示")
    void loginWrongCode() {
        mockVerify(0L);

        Result result = userService.login(form("000000"), null);

        assertFalse(result.getSuccess());
        assertEquals("验证码错误", result.getErrorMsg());
        verify(userMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("登录：验证码为空 -> 直接拒绝，不消耗失败次数")
    void loginBlankCode() {
        Result result = userService.login(form("  "), null);

        assertFalse(result.getSuccess());
        // 空验证码不该走 Lua：否则用户误点空提交也会消耗一次失败次数
        verify(stringRedisTemplate, never()).execute(any(RedisScript.class), anyList(), any(), any(), any());
    }

    // ---------------- 并发首登 ----------------

    @Test
    @DisplayName("并发首登：撞唯一索引后复用另一个线程已建好的用户，不报服务器异常")
    void concurrentFirstLoginReusesExistingUser() {
        mockVerify(1L);
        QueryChainWrapper<User> chain = mockQueryChain();
        User created = new User();
        created.setId(2001L);
        created.setPhone(PHONE);
        created.setNickName("并发建出来的");
        // 第 1 次查不到 -> 走创建；创建撞索引 -> catch 里第 2 次查到
        when(chain.one()).thenReturn(null, created);
        when(userMapper.insert(any(User.class))).thenThrow(new DuplicateKeyException("uniqe_key_phone"));
        when(jwtUtil.generate(any())).thenReturn("jwt-token");

        Result result = userService.login(form("123456"), null);

        assertTrue(result.getSuccess());
        assertEquals("jwt-token", result.getData());
    }

    @Test
    @DisplayName("并发首登：撞索引后又查不到记录 -> 原样抛出，不吞异常")
    void concurrentFirstLoginRethrowsWhenStillMissing() {
        mockVerify(1L);
        QueryChainWrapper<User> chain = mockQueryChain();
        when(chain.one()).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenThrow(new DuplicateKeyException("uniqe_key_phone"));

        assertThrows(DuplicateKeyException.class, () -> userService.login(form("123456"), null));
    }
}
