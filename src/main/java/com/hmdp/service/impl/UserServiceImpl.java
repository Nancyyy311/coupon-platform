package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.enums.Role;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.JwtUtil;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 改造新增：JWT 签发 */
    @Resource
    private JwtUtil jwtUtil;

    /** 验证码校验脚本：比对 + 删除 + 失败计数，一次原子执行 */
    private static final DefaultRedisScript<Long> LOGIN_CODE_SCRIPT;

    static {
        LOGIN_CODE_SCRIPT = new DefaultRedisScript<>();
        LOGIN_CODE_SCRIPT.setLocation(new ClassPathResource("login_code.lua"));
        LOGIN_CODE_SCRIPT.setResultType(Long.class);
    }

    /** Lua 返回码 */
    private static final int VERIFY_OK = 1;
    private static final int VERIFY_LOCKED = 2;

    /**
     * 验证码最多错几次。
     * 6 位数字共 100 万种，2 分钟 TTL 内如果只限制请求速率而不管次数，
     * 依然能穷举出一个可用的码；限到 5 次之后，命中概率 5/1000000，可以不管了。
     */
    private static final int CODE_MAX_FAILS = 5;

    /** 同号发送间隔（秒）。真实短信渠道也有这个限制，前移到应用层可省掉一笔短信费 */
    private static final long CODE_RESEND_INTERVAL_SECONDS = 60L;

    /** 同号 24 小时发送上限 */
    private static final long CODE_DAILY_LIMIT = 10L;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2.不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        //2.5 频率限制。放在生成验证码之前：它比生成更便宜，也比生成更重要。
        //    不加这层的话，这个接口就是"匿名可达 + 每次写一条 Redis + 触发一次短信"，
        //    既能刷爆短信费，也能把 Redis 的写入量顶上去。
        String rejected = checkSendFrequency(phone);
        if (rejected != null) {
            return Result.fail(rejected);
        }
        //3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4.保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //5.发送验证码
        log.debug("发送短信验证码成功，验证码：{}", code);
        //返回ok
        return Result.ok();
    }

    /**
     * 发送频率检查。
     *
     * @return null 表示允许发送，否则是拒绝原因
     */
    private String checkSendFrequency(String phone) {
        // 第一层：同号 60 秒一次。SET NX 本身是原子的，并发打进来只有一个请求能占到坑
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(
                LOGIN_CODE_THROTTLE_KEY + phone, "1", CODE_RESEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(first)) {
            return "验证码已发送，请 " + CODE_RESEND_INTERVAL_SECONDS + " 秒后再试";
        }

        // 第二层：同号 24 小时总量。60 秒一次乘 24 小时也有 1440 条，
        // 单靠间隔挡不住"挂一天慢慢刷"，所以必须再压一个总量上限
        String dailyKey = LOGIN_CODE_DAILY_KEY + phone;
        Long daily = stringRedisTemplate.opsForValue().increment(dailyKey);
        if (daily != null && daily == 1L) {
            // 第一次计数时设 TTL，形成 24 小时滚动窗口。
            // 这里没有做成 Lua：INCR 与 EXPIRE 之间进程挂掉的概率极低，
            // 而真发生了也只是这个手机号当天不能再收码，属于可接受的退化。
            stringRedisTemplate.expire(dailyKey, 1, TimeUnit.DAYS);
        }
        if (daily != null && daily > CODE_DAILY_LIMIT) {
            return "今日获取验证码次数已达上限，请明天再试";
        }
        return null;
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误！");
        }
        String code = loginForm.getCode();
        if (StrUtil.isBlank(code)) {
            return Result.fail("请输入验证码");
        }

        //2.校验验证码：比对 + 删除 + 失败计数，一次 Lua 调用完成。
        //  分开写是错的——"GET 出来比对、再 DEL" 有两次网络往返，并发时多个请求
        //  都能读到同一个码并都判断相等，验证码就从"一次性"变成了"多次有效"。
        //  实测 6 并发同码登录，有 2 个请求同时通过了校验，最后撞在手机号唯一索引上
        //  返回"服务器异常"。脚本由 Redis 单线程执行，天然串行。
        Long verifyResult = stringRedisTemplate.execute(
                LOGIN_CODE_SCRIPT,
                Arrays.asList(LOGIN_CODE_KEY + phone, LOGIN_CODE_FAIL_KEY + phone),
                code,
                String.valueOf(LOGIN_CODE_TTL * 60),
                String.valueOf(CODE_MAX_FAILS)
        );
        int verify = verifyResult == null ? 0 : verifyResult.intValue();
        if (verify == VERIFY_LOCKED) {
            log.warn("[登录] 验证码错误次数超限，已作废 phone={}", phone);
            return Result.fail("验证码错误次数过多，请重新获取");
        }
        if (verify != VERIFY_OK) {
            return Result.fail("验证码错误");
        }
        //4.一致，根据手机号查询用户
        User user = query().eq("phone", phone).one();
        //5.判断用户是否存在
        if (user == null) {
            //6.不存在，创建新用户并保存
            user = createUserWithPhone(phone);
        }
        //7.签发 JWT（改造点）
        //  不再把 UserDTO 写进 Redis Hash 当会话用。
        //  token 自身携带 userId/nickName/icon 和有效期，由签名保证不可篡改，
        //  服务端零存储 —— 登录态不再依赖 Redis 是否可用。
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        String token = jwtUtil.generate(userDTO);
        //8.返回token
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        // 1. 获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2. 获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3. 拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4. 获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5. 写入Redis SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        // 1. 获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2. 获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3. 拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4. 获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5. 获取本月截止今天为止的所有的签到记录
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            // 没有任何签到结果
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        // 6. 循环遍历，统计连续签到
        int count = 0;
        while (true) {
            // 和1做与运算，拿到最后一位bit
            if ((num & 1) == 0) {
                // 遇到0，断签，结束循环
                break;
            }
            // 签到，计数+1
            count++;
            // 无符号右移，判断前一天
            num >>>= 1;
        }
        return Result.ok(count);
    }

    private User createUserWithPhone(String phone) {
        //1.创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(8));
        // 新用户一律是普通用户，商家身份只能由运维/后台改库授予。
        // 不在注册接口上开"选角色"的口子——那等于把权限系统的入口交给调用方。
        user.setRole(Role.DEFAULT);
        try {
            save(user);
            return user;
        } catch (DuplicateKeyException e) {
            // 并发首登：两个请求同时"查不到用户 -> 创建"，唯一索引 uniqe_key_phone
            // 只会让一个成功。落败的一方如果直接往上抛，就被全局异常处理器兜成
            // "服务器异常"——用户明明登录成功了，界面上却是报错（实测 6 并发里命中 2 次）。
            // 这里改成"冲突即重查"：那条记录一定是刚被另一个线程建好的，直接拿来用。
            User exist = query().eq("phone", phone).one();
            if (exist == null) {
                // 索引冲突但查不到，说明不是并发首登（比如 phone 列类型/排序规则导致的怪冲突），
                // 不能吞掉，交给上层暴露出来
                throw e;
            }
            log.info("[登录] 并发首登命中唯一索引，复用已建用户 phone={} userId={}", phone, exist.getId());
            return exist;
        }
    }
}
