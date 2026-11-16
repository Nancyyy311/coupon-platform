package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 关注关系的完整性测试。
 *
 * 原实现有三个洞：
 *   ① 能关注自己 —— 之后发笔记会把笔记推给自己
 *   ② 能关注不存在的用户 —— 表上没有外键，脏数据会出现在共同关注里
 *   ③ 重复关注会插多条 —— tb_follow 只有主键，没有 (user_id, follow_user_id) 唯一索引
 */
@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SetOperations<String, String> setOps;

    @Mock
    private IUserService userService;

    @Mock
    private FollowMapper followMapper;

    private FollowServiceImpl followService;

    private static final Long ME = 1005L;
    private static final Long OTHER = 2001L;

    @BeforeEach
    void setUp() {
        followService = spy(new FollowServiceImpl());
        ReflectionTestUtils.setField(followService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(followService, "userService", userService);
        ReflectionTestUtils.setField(followService, "baseMapper", followMapper);

        UserDTO me = new UserDTO();
        me.setId(ME);
        UserHolder.saveUser(me);
    }

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    @Test
    @DisplayName("关注自己：直接拒绝，不写库不写 Redis")
    void cannotFollowSelf() {
        Result result = followService.follow(ME, true);

        assertFalse(result.getSuccess());
        assertEquals("不能关注自己", result.getErrorMsg());
        verify(followMapper, never()).insert(any(Follow.class));
    }

    @Test
    @DisplayName("关注不存在的用户：直接拒绝")
    void cannotFollowMissingUser() {
        when(userService.getById(999L)).thenReturn(null);

        Result result = followService.follow(999L, true);

        assertFalse(result.getSuccess());
        assertEquals("用户不存在", result.getErrorMsg());
        verify(followMapper, never()).insert(any(Follow.class));
    }

    @Test
    @DisplayName("关注成功：写库 + 写 Redis 投影")
    void followOk() {
        when(userService.getById(OTHER)).thenReturn(new User());
        when(followMapper.insert(any(Follow.class))).thenReturn(1);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOps);

        Result result = followService.follow(OTHER, true);

        assertTrue(result.getSuccess());
        verify(setOps).add("follows:" + ME, OTHER.toString());
    }

    @Test
    @DisplayName("重复关注：唯一索引冲突按幂等成功处理，Redis 也必须写进去")
    void duplicateFollowIsIdempotent() {
        when(userService.getById(OTHER)).thenReturn(new User());
        when(followMapper.insert(any(Follow.class)))
                .thenThrow(new DuplicateKeyException("uk_user_follow"));
        when(stringRedisTemplate.opsForSet()).thenReturn(setOps);

        Result result = followService.follow(OTHER, true);

        // 关键：不能因为索引挡住就报错，也不能跳过 Redis 写入
        assertTrue(result.getSuccess());
        verify(setOps).add("follows:" + ME, OTHER.toString());
    }

    @Test
    @DisplayName("取关：库和 Redis 都要清，且不因影响行数是 0 就跳过 Redis")
    void unfollowClearsBoth() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOps);
        when(followMapper.delete(any())).thenReturn(0);   // 本来就没关注

        Result result = followService.follow(OTHER, false);

        assertTrue(result.getSuccess());
        verify(setOps).remove("follows:" + ME, OTHER.toString());
    }

    // ---------------- 共同关注：缓存缺失不能答成"没有" ----------------

    @SuppressWarnings("unchecked")
    private QueryChainWrapper<Follow> mockQueryChain(List<Follow> rows) {
        QueryChainWrapper<Follow> chain = mock(QueryChainWrapper.class);
        doReturn(chain).when(followService).query();
        when(chain.eq(anyString(), any())).thenReturn(chain);
        when(chain.list()).thenReturn(rows);
        return chain;
    }

    @Test
    @DisplayName("共同关注：Redis 两个 Set 都不存在 -> 先从库重建，再求交集")
    void followCommonsRebuildsMissingSets() {
        Follow meFollowsOther = new Follow();
        meFollowsOther.setUserId(ME);
        meFollowsOther.setFollowUserId(OTHER);
        Follow otherFollowsThird = new Follow();
        otherFollowsThird.setUserId(OTHER);
        otherFollowsThird.setFollowUserId(3001L);
        QueryChainWrapper<Follow> chain = mockQueryChain(Collections.singletonList(meFollowsOther));
        // 两次重建分别对应"我关注的"和"对方关注的"
        when(chain.list()).thenReturn(
                Collections.singletonList(meFollowsOther),
                Collections.singletonList(otherFollowsThird));

        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.intersect("follows:" + ME, "follows:" + OTHER))
                .thenReturn(new HashSet<>(Collections.singletonList(OTHER.toString())));
        User u = new User();
        u.setId(OTHER);
        u.setNickName("被共同关注的人");
        when(userService.listByIds(any())).thenReturn(Collections.singletonList(u));

        Result result = followService.followCommons(OTHER);

        assertTrue(result.getSuccess());
        // 两个方向的 Set 都要重建，缺一个交集就恒为空
        verify(setOps).add("follows:" + ME, OTHER.toString());
        verify(setOps).add("follows:" + OTHER, "3001");
        assertEquals(1, ((List<?>) result.getData()).size());
    }

    @Test
    @DisplayName("共同关注：Set 都在且交集为空 -> 正常返回空列表，不回源")
    void followCommonsEmptyIntersection() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.intersect(anyString(), anyString())).thenReturn(Collections.emptySet());

        Result result = followService.followCommons(OTHER);

        assertTrue(result.getSuccess());
        assertEquals(0, ((List<?>) result.getData()).size());
        // key 都在，就不该再查库
        verify(followService, never()).query();
    }
}
