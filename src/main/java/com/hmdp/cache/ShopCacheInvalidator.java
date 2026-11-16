package com.hmdp.cache;

import com.hmdp.utils.CacheClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

/**
 * 商户缓存失效（高性能 + 一致性）。
 *
 * 原版和那份魔改资料都缺的一环：
 *   加了 L1 之后，多实例下会出现"A 机器已更新、B 机器还在返回旧数据"。
 *   只删 Redis 是不够的，因为 B 机器的 Caffeine 里那份根本不知道 Redis 变了。
 *
 * 完整做法是四步，缺一步都会留下窗口：
 *   ① 先更新数据库（不是先删缓存）
 *   ② **事务提交之后**再删缓存（提交前删，其他节点会立刻回源读到未提交的旧值并写回缓存，
 *      而且这次写回的 TTL 是完整的 30 分钟，脏数据会活很久）
 *   ③ 删 Redis + 删本机 L1 + 广播给其他实例清各自的 L1
 *   ④ 延迟再删一次（延迟双删），兜住"读线程在删除之后才把旧值写回缓存"的窗口
 *
 * 为什么用 pub/sub 而不是 MQ 广播：
 *   失效通知是"丢了也无所谓"的消息——L1 只有 5 秒 TTL，最坏情况就是多陈旧 5 秒。
 *   用 MQ 要额外维护交换机、队列、消费确认，为一个可以丢的消息付出可靠投递的成本，
 *   不划算。这个判断本身就是个很好的面试回答：不是所有消息都值得用 MQ。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopCacheInvalidator {

    /** 广播频道名 */
    public static final String CHANNEL = "cache:invalidate:shop";

    /**
     * 第二次删除的延迟。
     *
     * 取 500ms 的依据：要覆盖"读线程从查到旧值到写回缓存"这段耗时（一次 DB 查询 + JSON 序列化）。
     * 这个值不能无限大——延迟越久，第二次删除时旧值已经被人读走一轮；也不能太小，
     * 太小则读线程还没写回，删了个寂寞。500ms 是这两个约束的折中，不是拍脑袋的魔法数。
     */
    public static final long SECOND_EVICT_DELAY_MS = 500L;

    private final StringRedisTemplate stringRedisTemplate;
    private final CacheClient cacheClient;
    private final TaskScheduler taskScheduler;

    /** 商户数据变更后调用：删 Redis + 删本机 L1 + 广播 */
    public void evict(Long shopId) {
        doEvict(shopId);
    }

    /**
     * 延迟再删一次（延迟双删的第二次）。
     *
     * 只删一次，不会自我循环。调度失败不影响主流程——第一次删除已经完成，
     * 剩下的窗口由 L2 的 TTL 和 L1 的 5 秒过期兜底。
     */
    public void evictLater(Long shopId, long delayMs) {
        try {
            taskScheduler.schedule(() -> {
                try {
                    doEvict(shopId);
                    log.debug("[缓存双删-第二删] shopId={} 延迟 {}ms 后已再次清除", shopId, delayMs);
                } catch (Exception e) {
                    // 第二次删除失败不升级为错误：此时数据大概率已经一致，只是窗口没被完全堵死
                    log.warn("[缓存双删-第二删失败] shopId={}，等待 TTL 兜底", shopId, e);
                }
            }, Instant.now().plusMillis(delayMs));
        } catch (Exception e) {
            log.warn("[缓存双删-调度失败] shopId={}，跳过第二次删除", shopId, e);
        }
    }

    private void doEvict(Long shopId) {
        String key = CACHE_SHOP_KEY + shopId;
        stringRedisTemplate.delete(key);
        cacheClient.invalidateLocal(key);
        stringRedisTemplate.convertAndSend(CHANNEL, String.valueOf(shopId));
        log.debug("[缓存失效] shopId={} 已删除 Redis + 本机 L1，并广播至其他实例", shopId);
    }
}
