package com.hmdp.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 本地售罄标识（高并发第一层拦截）。
 *
 * 解决的问题：券卖完之后，仍然会有人不停点"立即抢购"。
 * 每个请求都要走一次 Redis 才能知道"没库存了"，这部分流量纯属浪费。
 *
 * 做法：某张券在 Lua 返回 1（库存不足）后，在本机内存里记一个标记，30 秒内
 * 直接返回售罄，一个 Redis 命令都不发。
 *
 * 代价（必须知道，面试会问）：
 *   30 秒内如果有人退单，这张券其实又有库存了，但本机会继续判售罄，
 *   直到标记过期。这是"用 30 秒的可用性换掉绝大部分无效流量"的取舍。
 *   如果业务要求退单立即可抢，把 TTL 调到 3~5 秒，或退单时主动 remove。
 *
 * 注意：这个标记是进程内的，多实例各一份，不能用它做任何正确性判断，
 *      它只负责"挡流量"，正确性由 Redis 的 Lua 保证。
 */
@Slf4j
@Component
public class LocalSoldOutGuard {

    /** 售罄标记存活时间，秒 */
    private static final long SOLD_OUT_TTL_SECONDS = 30;

    /** 一键开关：出问题时不改代码，配置里关掉即可退化为"每次都问 Redis" */
    @Value("${app.soldout.enabled:true}")
    private boolean enabled;

    private final Cache<Long, Boolean> soldOutCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(SOLD_OUT_TTL_SECONDS, TimeUnit.SECONDS)
            .build();

    /** 是否已在本机标记为售罄 */
    public boolean isSoldOut(Long voucherId) {
        if (!enabled) {
            return false;
        }
        return Boolean.TRUE.equals(soldOutCache.getIfPresent(voucherId));
    }

    /** Lua 返回库存不足时调用 */
    public void markSoldOut(Long voucherId) {
        if (!enabled) {
            return;
        }
        soldOutCache.put(voucherId, Boolean.TRUE);
        log.debug("[本地售罄标识] voucherId={} 已标记，{}s 内不再访问 Redis", voucherId, SOLD_OUT_TTL_SECONDS);
    }

    /** 退单回补库存、或运营补库存时调用，让本机立刻恢复可抢状态 */
    public void clear(Long voucherId) {
        soldOutCache.invalidate(voucherId);
    }
}
