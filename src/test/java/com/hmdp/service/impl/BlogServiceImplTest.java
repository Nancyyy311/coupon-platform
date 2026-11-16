package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 点赞与写扩散的单元测试。
 *
 * 两个都是实测过的真实缺陷：
 *   ① 点赞用"先查 score 再判断"，并发下 1 个人能点出 2 个赞（ZADD 幂等，但 DB 执行了两次 +1）
 *   ② 发笔记时按粉丝数逐条 ZADD，1000 个粉丝就是 1000 次网络往返
 */
@ExtendWith(MockitoExtension.class)
class BlogServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOps;

    @Mock
    private IUserService userService;

    @Mock
    private IFollowService followService;

    @Mock
    private BlogMapper blogMapper;

    private BlogServiceImpl blogService;

    private static final Long USER_ID = 1005L;
    private static final Long BLOG_ID = 7L;

    @BeforeEach
    void setUp() {
        blogService = spy(new BlogServiceImpl());
        ReflectionTestUtils.setField(blogService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(blogService, "userService", userService);
        ReflectionTestUtils.setField(blogService, "followService", followService);
        ReflectionTestUtils.setField(blogService, "baseMapper", blogMapper);

        UserDTO user = new UserDTO();
        user.setId(USER_ID);
        user.setNickName("测试用户");
        UserHolder.saveUser(user);
    }

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    @SuppressWarnings("unchecked")
    private UpdateChainWrapper<Blog> mockUpdateChain() {
        UpdateChainWrapper<Blog> chain = mock(UpdateChainWrapper.class);
        doReturn(chain).when(blogService).update();
        when(chain.setSql(anyString())).thenReturn(chain);
        when(chain.eq(anyString(), any())).thenReturn(chain);
        return chain;
    }

    private void mockScore(Double score) {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.score(anyString(), anyString())).thenReturn(score);
    }

    private void mockLua(Long... results) {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .thenReturn(results[0], Arrays.copyOfRange(results, 1, results.length));
    }

    @Test
    @DisplayName("点赞：脚本返回 1（切换后是已点赞）-> 计数 +1")
    void likeFirstTime() {
        mockLua(1L);
        UpdateChainWrapper<Blog> chain = mockUpdateChain();
        when(chain.update()).thenReturn(true);

        Result result = blogService.likeBlog(BLOG_ID);

        assertTrue(result.getSuccess());
        verify(chain).setSql("liked = liked + 1");
    }

    @Test
    @DisplayName("连点两次：脚本串行返回 1、0 -> 计数先 +1 再 -1，不会出现 +2")
    void twoTapsFlipTwice() {
        mockLua(1L, 0L);
        UpdateChainWrapper<Blog> chain = mockUpdateChain();
        when(chain.gt(anyString(), any())).thenReturn(chain);
        when(chain.update()).thenReturn(true, true);

        blogService.likeBlog(BLOG_ID);
        blogService.likeBlog(BLOG_ID);

        // 关键点：状态翻转几次就改几次，且一次 +1 一次 -1，净变化为 0
        verify(chain).setSql("liked = liked + 1");
        verify(chain).setSql("liked = liked - 1");
    }

    @Test
    @DisplayName("取消赞：脚本返回 0 -> 计数 -1，且带 liked > 0 条件，不会减成负数")
    void unlikeKeepsNonNegative() {
        mockLua(0L);
        UpdateChainWrapper<Blog> chain = mockUpdateChain();
        when(chain.gt(anyString(), any())).thenReturn(chain);
        when(chain.update()).thenReturn(true);

        Result result = blogService.likeBlog(BLOG_ID);

        assertTrue(result.getSuccess());
        verify(chain).setSql("liked = liked - 1");
        verify(chain).gt("liked", 0);
    }

    @Test
    @DisplayName("取消赞但库里计数已是 0：不报错，也不去动 Redis（语义已经一致）")
    void unlikeWhenCounterAlreadyZero() {
        mockLua(0L);
        UpdateChainWrapper<Blog> chain = mockUpdateChain();
        when(chain.gt(anyString(), any())).thenReturn(chain);
        when(chain.update()).thenReturn(false);   // 0 行受影响

        Result result = blogService.likeBlog(BLOG_ID);

        assertTrue(result.getSuccess());
        // 这时候 Redis 是"未点赞"、库里是 0，两边已经一致，不该再回滚
        verify(stringRedisTemplate, never()).opsForZSet();
    }

    @Test
    @DisplayName("点赞不存在的笔记：DB 影响 0 行 -> 撤回 Redis 点赞并报错")
    void likeMissingBlogRollsBackRedis() {
        mockLua(1L);
        UpdateChainWrapper<Blog> chain = mockUpdateChain();
        when(chain.update()).thenReturn(false);   // liked + 1 影响 0 行 = id 不存在
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOps);

        Result result = blogService.likeBlog(999L);

        assertFalse(result.getSuccess());
        assertEquals("笔记不存在！", result.getErrorMsg());
        // 必须把刚加上的赞撤掉，否则点赞列表里躺着一个不存在的笔记
        verify(zSetOps).remove("blog:liked:999", USER_ID.toString());
    }

    @Test
    @DisplayName("脚本没返回（Redis 异常）：不动 DB，直接报可重试")
    void likeReturnsNullWhenRedisFails() {
        mockLua((Long) null);

        Result result = blogService.likeBlog(BLOG_ID);

        assertFalse(result.getSuccess());
        verify(blogService, never()).update();
    }

    // ---------------- 写扩散：一次 pipeline 而不是 N 次往返 ----------------

    @SuppressWarnings("unchecked")
    private QueryChainWrapper<Follow> mockFollowQuery(List<Follow> follows) {
        QueryChainWrapper<Follow> chain = mock(QueryChainWrapper.class);
        when(followService.query()).thenReturn(chain);
        when(chain.eq(anyString(), any())).thenReturn(chain);
        when(chain.list()).thenReturn(follows);
        return chain;
    }

    @Test
    @DisplayName("发笔记：3 个粉丝只发一次 pipeline，内部写 3 条 feed")
    void saveBlogPushesFeedInOnePipeline() {
        List<Follow> follows = new ArrayList<>();
        for (long fid = 1; fid <= 3; fid++) {
            Follow f = new Follow();
            f.setUserId(fid);
            follows.add(f);
        }
        mockFollowQuery(follows);
        when(blogMapper.insert(any(Blog.class))).thenAnswer(inv -> {
            Blog b = inv.getArgument(0);
            b.setId(99L);
            return 1;
        });

        final int[] zaddCount = {0};
        doAnswer(inv -> {
            RedisCallback<?> callback = inv.getArgument(0);
            StringRedisConnection conn = mock(StringRedisConnection.class);
            callback.doInRedis(conn);
            verify(conn, times(3)).zAdd(anyString(), anyDouble(), anyString());
            zaddCount[0] = 3;
            return null;
        }).when(stringRedisTemplate).executePipelined(any(RedisCallback.class));

        Blog blog = new Blog();
        blog.setTitle("测试笔记");
        Result result = blogService.saveBlog(blog);

        assertTrue(result.getSuccess());
        assertEquals(99L, result.getData());
        verify(stringRedisTemplate, times(1)).executePipelined(any(RedisCallback.class));
        assertEquals(3, zaddCount[0]);
    }

    @Test
    @DisplayName("发笔记：没有粉丝时连 pipeline 都不开")
    void saveBlogWithoutFollowersSkipsPipeline() {
        mockFollowQuery(Collections.emptyList());
        when(blogMapper.insert(any(Blog.class))).thenAnswer(inv -> {
            Blog b = inv.getArgument(0);
            b.setId(100L);
            return 1;
        });

        Blog blog = new Blog();
        blog.setTitle("没有粉丝也照样发得出去");
        blogService.saveBlog(blog);

        verify(stringRedisTemplate, never()).executePipelined(any(RedisCallback.class));
    }

    @Test
    @DisplayName("发笔记：落库失败直接返回，不给粉丝推一个不存在的笔记")
    void saveBlogInsertFailure() {
        when(blogMapper.insert(any(Blog.class))).thenReturn(0);
        Blog blog = new Blog();
        blog.setTitle("标题合法但落库失败");

        Result result = blogService.saveBlog(blog);

        assertFalse(result.getSuccess());
        verify(stringRedisTemplate, never()).executePipelined(any(RedisCallback.class));
        verify(followService, never()).query();
    }

    // ---------------- 列表批量补全：把 N+1 压成 2 次往返 ----------------

    private Blog blogOf(long id, long userId) {
        Blog b = new Blog();
        b.setId(id);
        b.setUserId(userId);
        return b;
    }

    private User userOf(long id, String nickName) {
        User u = new User();
        u.setId(id);
        u.setNickName(nickName);
        u.setIcon("icon-" + id);
        return u;
    }

    @Test
    @DisplayName("列表补作者：3 条笔记 2 个不同作者 -> 只查 1 次库，作者可重复复用")
    @SuppressWarnings("unchecked")
    void fillUsersQueriesOnce() {
        List<Blog> blogs = Arrays.asList(blogOf(1L, 1005L), blogOf(2L, 1006L), blogOf(3L, 1005L));
        when(userService.listByIds(any())).thenReturn(Arrays.asList(
                userOf(1005L, "甲"), userOf(1006L, "乙")));

        ReflectionTestUtils.invokeMethod(blogService, "fillUsers", blogs);

        // 关键：不是 3 次 getById
        verify(userService, times(1)).listByIds(any());
        verify(userService, never()).getById(any());
        assertEquals("甲", blogs.get(0).getName());
        assertEquals("乙", blogs.get(1).getName());
        assertEquals("甲", blogs.get(2).getName());
        assertEquals("icon-1005", blogs.get(0).getIcon());
    }

    @Test
    @DisplayName("列表补作者：作者已被注销 -> 降级为占位文案，不能 NPE 让整页 500")
    @SuppressWarnings("unchecked")
    void fillUsersToleratesMissingAuthor() {
        List<Blog> blogs = Collections.singletonList(blogOf(1L, 9999L));
        when(userService.listByIds(any())).thenReturn(Collections.emptyList());

        ReflectionTestUtils.invokeMethod(blogService, "fillUsers", blogs);

        assertEquals("用户已注销", blogs.get(0).getName());
        assertEquals("", blogs.get(0).getIcon());
    }

    @Test
    @DisplayName("列表补点赞态：3 条笔记只发 1 次 pipeline，结果按顺序回填")
    @SuppressWarnings("unchecked")
    void fillLikedUsesSinglePipeline() {
        List<Blog> blogs = Arrays.asList(blogOf(1L, 1005L), blogOf(2L, 1005L), blogOf(3L, 1005L));
        List<Object> scores = new ArrayList<>(Arrays.asList(null, 1.0d, null));

        doAnswer(inv -> {
            RedisCallback<?> callback = inv.getArgument(0);
            StringRedisConnection conn = mock(StringRedisConnection.class);
            callback.doInRedis(conn);
            verify(conn, times(3)).zScore(anyString(), anyString());
            return scores;
        }).when(stringRedisTemplate).executePipelined(any(RedisCallback.class));

        ReflectionTestUtils.invokeMethod(blogService, "fillLiked", blogs);

        verify(stringRedisTemplate, times(1)).executePipelined(any(RedisCallback.class));
        assertEquals(Boolean.FALSE, blogs.get(0).getIsLike());
        assertEquals(Boolean.TRUE, blogs.get(1).getIsLike());
        assertEquals(Boolean.FALSE, blogs.get(2).getIsLike());
    }

    @Test
    @DisplayName("列表补点赞态：未登录时一律 false，一次 Redis 都不打")
    void fillLikedWithoutLoginSkipsRedis() {
        UserHolder.removeUser();
        List<Blog> blogs = Collections.singletonList(blogOf(1L, 1005L));

        ReflectionTestUtils.invokeMethod(blogService, "fillLiked", blogs);

        assertEquals(Boolean.FALSE, blogs.get(0).getIsLike());
        verify(stringRedisTemplate, never()).executePipelined(any(RedisCallback.class));
    }
}
