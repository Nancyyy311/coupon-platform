package com.hmdp.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.utils.RedisConstants.FEED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService followService;

    /**
     * 点赞状态切换脚本：ZADD NX / ZREM 的返回值就是"状态是否真的变了"。
     * 用它当唯一判定，而不是先查 score 再判断——后者在并发下会让 1 个人点出 2 个赞。
     */
    private static final DefaultRedisScript<Long> LIKE_SCRIPT;

    static {
        LIKE_SCRIPT = new DefaultRedisScript<>();
        LIKE_SCRIPT.setLocation(new ClassPathResource("blog_like.lua"));
        LIKE_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 批量补作者与点赞态：10 条笔记 1 次 SQL + 1 次 pipeline，
        // 而不是逐条 getById / ZSCORE（那是 11 条 SQL + 10 次 Redis 往返）
        fillUsers(records);
        fillLiked(records);
        return Result.ok(records);
    }

    @Override
    public Result queryBlogById(Long id) {
        //1.查询blog
        Blog blog = getById(id);
        if(blog == null ){
            return  Result.fail("笔记不存在！");
        }
        //2.查询blog有关的用户
        queryBlogUser(blog);
        //3.查询blog是否被点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog){
        //1.获取登录用户
        UserDTO user = UserHolder.getUser();
        if(user == null){
            //用户未登录，无需查询是否点赞
            return;
        }
        Long userId = user.getId();
        // 2.判断当前用户是否已经点赞
        String key = "blog:liked:" + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key,userId.toString());
        blog.setIsLike(score != null);
    }

    @Override
    public Result likeBlog(Long id) {
        //1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        String key = BLOG_LIKED_KEY + id;

        // 2.原子切换 Redis 状态，返回值就是"切换之后的状态"：1 已点赞 / 0 未点赞。
        //   这一版和上一版的区别在于：上一版在 Lua 外面先 ZSCORE 判断"这次是点还是取消"，
        //   那是一个读-改-写竞态——并发下 A 刚点上，B 读到"已点赞"就去取消，净变化变 0，
        //   一个人连点 5 次可能既没点上也没取消。现在"读状态 + 切换"都在脚本里，
        //   Redis 单线程执行，读到的必然是切换前的真实状态。
        Long newState = stringRedisTemplate.execute(
                LIKE_SCRIPT,
                Collections.singletonList(key),
                userId.toString(),
                String.valueOf(System.currentTimeMillis())
        );
        if (newState == null) {
            // 脚本没拿到返回值（连接异常），这时候 Redis 状态不确定，不能盲改 DB
            return Result.fail("点赞失败，请重试");
        }
        boolean liked = newState == 1L;

        // 3.更新数据库计数：一次请求对应一次状态翻转，所以这里必然是 +1 或 -1 之一
        boolean dbOk = liked
                ? update().setSql("liked = liked + 1").eq("id", id).update()
                // 取消赞加 liked > 0 条件：库里的计数已经到 0 时不再往下减
                : update().setSql("liked = liked - 1").eq("id", id).gt("liked", 0).update();

        if (!dbOk && liked) {
            // liked + 1 只要 id 存在就必然影响 1 行，影响 0 行只可能是这篇笔记不存在。
            // Redis 已经加过赞了，必须撤回，否则点赞列表里会出现一个不存在的笔记。
            stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            return Result.fail("笔记不存在！");
        }
        // 取消赞影响 0 行只可能有两种情况：笔记不存在，或 liked 已经是 0。
        // 两种情况下 Redis 都已经是"未点赞"、DB 都不会继续减，语义一致，不需要补偿。
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        //1.查询top5的点赞用户 zrange key 0 4
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key,0,4);
        if(top5 == null || top5.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        //2.解析出其中的用户id
        //   这里刻意用 for 而不是 Stream：集合长度恒为 5 以内，Stream 省不了几行，
        //   却要引入惰性求值、装箱、Collectors 这些额外概念。代码是给人看的，
        //   能用最直白的写法说清楚就不要用"更高级"的写法。
        List<Long> ids = new ArrayList<>(top5.size());
        for (String userIdStr : top5) {
            ids.add(Long.valueOf(userIdStr));
        }
        String idStr = StrUtil.join(",",ids);
        //3.根据用户id查询用户，按点赞时间倒序（FIELD 保序）
        List<User> users = userService.query()
                .in("id", ids)
                .last("ORDER BY FIELD(id," + idStr + ")")
                .list();
        List<UserDTO> userDTOS = new ArrayList<>(users.size());
        for (User user : users) {
            userDTOS.add(BeanUtil.copyProperties(user, UserDTO.class));
        }
        //4.返回
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 1. 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 2. 入参校验。
        //    title/content 在库里是 varchar(255)/varchar(2048)，超长时 MySQL 抛
        //    "Data too long for column"，被全局异常处理器兜成"服务器异常"——
        //    用户写多了几个字，得到的是"服务器异常"，无从下手。
        //    在这里挡住，报错就能直接指出是哪一项。
        if (StrUtil.isBlank(blog.getTitle())) {
            return Result.fail("标题不能为空");
        }
        if (blog.getTitle().length() > 255) {
            return Result.fail("标题最多 255 个字符");
        }
        if (blog.getContent() != null && blog.getContent().length() > 2048) {
            return Result.fail("正文最多 2048 个字符");
        }
        if (blog.getImages() != null && blog.getImages().length() > 2048) {
            return Result.fail("图片数量超出限制");
        }
        blog.setUserId(user.getId());
        // 3. 保存探店笔记
        boolean isSuccess = save(blog);
        if(!isSuccess){
            return Result.fail("新增笔记失败!");
        }
        // 4. 查询笔记作者的所有粉丝 select * from tb_follow where follow_user_id = ?
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        // 5. 推送笔记id给所有粉丝
        //    用 pipeline 一次往返写完，而不是 for 循环里发 N 次命令。
        //    粉丝 1000 人时：前者 1 次 RTT，后者 1000 次——发笔记是同步接口，
        //    1000 次 RTT 就是几百毫秒的响应时间，而且占着 Tomcat 线程不放。
        if (!follows.isEmpty()) {
            long now = System.currentTimeMillis();
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                StringRedisConnection conn = (StringRedisConnection) connection;
                for (Follow follow : follows) {
                    conn.zAdd(FEED_KEY + follow.getUserId(), now, blog.getId().toString());
                }
                return null;
            });
        }
        // 6. 返回id
        return Result.ok(blog.getId());

    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 1. 获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2. 查询收件箱 ZREVRANGEBYSCORE key Max Min LIMIT offset count
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        // 3. 非空判断
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        // 4. 解析数据: blogId、minTime（时间戳）、offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            // 4.1. 获取id
            ids.add(Long.valueOf(tuple.getValue()));
            // 4.2. 获取分数(时间戳)
            long time = tuple.getScore().longValue();
            if(time == minTime){
                os++;
            }else{
                minTime = time;
                os = 1;
            }
        }
        // 5. 根据id查询blog
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();

        // 5. 批量补作者与点赞态（同 queryHotBlog：把 N+1 压成 2 次往返）
        fillUsers(blogs);
        fillLiked(blogs);
        // 6. 封装并返回
        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(os);
        r.setMinTime(minTime);

        return Result.ok(r);

    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        // 作者被注销/数据不一致时，原版这里直接 user.getNickName()，NPE 会被全局处理器
        // 兜成"服务器异常"——一条脏数据能让整个列表页 500。降级成占位文案即可。
        if (user == null) {
            blog.setName("用户已注销");
            blog.setIcon("");
            return;
        }
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    /**
     * 批量补作者信息：一条 IN 查询替代 N 次 getById。
     *
     * 为什么值得单独写：列表接口一次返回 10 条笔记，原版对每条都发一条 select，
     * 10 条就是 10 次数据库往返（N+1）。粉丝 1000 人时这类放大更明显。
     * 这里把 user_id 去重后一次查回来，用 Map 回填，往返次数与列表长度无关。
     */
    private void fillUsers(List<Blog> blogs) {
        if (blogs == null || blogs.isEmpty()) {
            return;
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (Blog blog : blogs) {
            if (blog.getUserId() != null) {
                userIds.add(blog.getUserId());
            }
        }
        if (userIds.isEmpty()) {
            return;
        }
        List<User> users = userService.listByIds(userIds);
        Map<Long, User> userMap = new HashMap<>(users.size() * 2);
        for (User user : users) {
            userMap.put(user.getId(), user);
        }
        for (Blog blog : blogs) {
            User user = userMap.get(blog.getUserId());
            if (user == null) {
                blog.setName("用户已注销");
                blog.setIcon("");
            } else {
                blog.setName(user.getNickName());
                blog.setIcon(user.getIcon());
            }
        }
    }

    /**
     * 批量补"当前用户是否点过赞"：一次 pipeline 替代 N 次 ZSCORE。
     *
     * 这里也可以用 ZMSCORE 一条命令搞定，但 Spring Data Redis 2.x 的 ZSetOperations
     * 没有多 key 版本，用 pipeline 拿到的效果一样（1 次 RTT、N 条命令）。
     */
    private void fillLiked(List<Blog> blogs) {
        if (blogs == null || blogs.isEmpty()) {
            return;
        }
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 未登录：点赞态一律 false，也不用打 Redis
            for (Blog blog : blogs) {
                blog.setIsLike(false);
            }
            return;
        }
        String member = user.getId().toString();
        List<Object> scores = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection conn = (StringRedisConnection) connection;
            for (Blog blog : blogs) {
                conn.zScore(BLOG_LIKED_KEY + blog.getId(), member);
            }
            return null;
        });
        // pipeline 的结果按命令顺序返回，与 blogs 下标一一对应
        for (int i = 0; i < blogs.size(); i++) {
            blogs.get(i).setIsLike(scores.get(i) != null);
        }
    }
}
