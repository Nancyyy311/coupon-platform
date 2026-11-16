package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.hmdp.utils.RedisConstants.FOLLOWS_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        //1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        if (followUserId == null) {
            return Result.fail("参数不能为空");
        }
        // 自己关注自己没有业务意义，但会实打实地插一条记录：
        // 之后发笔记时"推给所有粉丝"会把自己也算进去，feed 里出现自己刚发的笔记。
        if (userId.equals(followUserId)) {
            return Result.fail("不能关注自己");
        }
        String key = FOLLOWS_KEY + userId;

        //1.判断到底是关注还是取关
        if (isFollow) {
            // 目标用户必须存在。表上没有外键，不校验就会留下"关注了一个不存在的人"，
            // 这个人还会出现在共同关注列表里，但点进去查不到用户。
            if (userService.getById(followUserId) == null) {
                return Result.fail("用户不存在");
            }
            //2.关注，新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            try {
                save(follow);
            } catch (DuplicateKeyException e) {
                // 重复关注。uk_user_follow 挡住的那条已经存在，语义上就是"已经关注了"，
                // 按幂等成功处理——不能报错，否则前端连点两次会看到一次失败。
                log.debug("[关注] 重复关注，幂等返回 userId={} followUserId={}", userId, followUserId);
            }
            // 3. 写 Redis 放在 try 之外：无论这次是新插入还是被索引挡住，
            //    这个 Set 里都必须有这个 id，否则共同关注会算漏
            stringRedisTemplate.opsForSet().add(key, followUserId.toString());
        } else {
            //3.取关，删除
            remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId).eq("follow_user_id", followUserId));
            // 取关不看影响行数：本来就没关注时删 0 行也是正确结果，
            // Redis 侧同样按"结果应该是什么"去写，而不是按"改了几行"去写
            stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        //1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        //2.查询是否关注
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        //3.判断
        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long id) {
        // 1. 获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2. 求交集
        String key = FOLLOWS_KEY + userId;
        String key2 = FOLLOWS_KEY + id;
        // 交集之前先确认两个 Set 真的在 Redis 里。
        // 这个 Set 只是数据库的投影：Redis 被清空/换实例之后它是空的，
        // 空交集算出来的结论是"没有共同关注"——那不是业务事实，是缓存缺失。
        // 所以缺了就按库重建，宁可多查一次库，也不要把"我不知道"答成"没有"。
        ensureFollowSet(userId);
        ensureFollowSet(id);
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        if (intersect == null || intersect.isEmpty()) {
            // 无交集
            return Result.ok(Collections.emptyList());
        }
        // 3. 解析id集合
        //    用 for 不用 Stream：只做"字符串转 Long"一件事，
        //    Stream 写法省不了行数，反而多一层惰性求值的阅读成本。
        List<Long> ids = new ArrayList<>(intersect.size());
        for (String idStr : intersect) {
            ids.add(Long.valueOf(idStr));
        }
        // 4. 查询用户
        List<User> userList = userService.listByIds(ids);
        List<UserDTO> users = new ArrayList<>(userList.size());
        for (User user : userList) {
            users.add(BeanUtil.copyProperties(user, UserDTO.class));
        }
        return Result.ok(users);
    }

    /**
     * 确保 "{follows}:{userId}" 这个 Set 在 Redis 里存在，不存在就从数据库重建。
     *
     * 只在缺 key 时回源，命中时就是一次 EXISTS（比一次 SELECT 便宜得多）。
     * 用户确实谁也没关注时，这里会退化成"每次查一次库"——那是低频接口，
     * 用一次索引查询换来"不会把缓存缺失答成没有共同关注"，是划算的。
     */
    private void ensureFollowSet(Long userId) {
        String key = FOLLOWS_KEY + userId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            return;
        }
        List<Follow> rows = query().eq("user_id", userId).list();
        if (rows.isEmpty()) {
            return;
        }
        String[] ids = new String[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            ids[i] = rows.get(i).getFollowUserId().toString();
        }
        stringRedisTemplate.opsForSet().add(key, ids);
        log.info("[共同关注] Redis 集合缺失，已从库重建 userId={} size={}", userId, ids.length);
    }
}
