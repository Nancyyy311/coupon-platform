package com.hmdp.utils;

import com.hmdp.cache.LocalSoldOutGuard;
import com.hmdp.monitor.SeckillMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Redis 库存操作（高可用补偿入口）。
 *
 * 所有"把库存还回去"的动作都收敛到这里，避免多处散落的回补逻辑
 * 各写各的、有的 SREM 有的忘了 SREM，最后 Redis 里留下无法解释的数据。
 *
 * 两个模式（对应 rollback.lua 的 ARGV[4]）：
 *   FULL  —— 用户最终没拿到券：库存 +1 且释放占位（发送失败 / 死信 / 超时关单）
 *   STOCK —— 用户其实已有有效订单：只把多扣的库存还回去，占位必须保留
 *            （唯一索引拦下的重复下单，如果连占位一起删了，他会一直抢、一直被拦）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockRedisService {

    /** 完整回滚：还库存 + 释放一人一单占位 */
    public static final String MODE_FULL = "full";
    /** 只还库存：占位保留（用户已有有效订单） */
    public static final String MODE_STOCK_ONLY = "stock";

    private static final DefaultRedisScript<Long> ROLLBACK_SCRIPT;

    static {
        ROLLBACK_SCRIPT = new DefaultRedisScript<>();
        ROLLBACK_SCRIPT.setLocation(new ClassPathResource("rollback.lua"));
        ROLLBACK_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final LocalSoldOutGuard localSoldOutGuard;
    private final SeckillMetrics metrics;

    /**
     * 完整回滚：库存 +1 并释放占位。用户下次还能抢这张券。
     *
     * @param orderId 幂等键。同一笔订单重复调用只生效一次（MQ 至少一次投递、
     *                confirm-nack 与 send 异常并发触发时都会走到这里）
     */
    public void rollback(Long orderId, Long voucherId, Long userId) {
        rollback(orderId, voucherId, userId, MODE_FULL);
    }

    /**
     * 回补入口。
     *
     * @return true 表示本次真的执行了回补；false 表示被幂等守卫拦下（已回补过）
     */
    public boolean rollback(Long orderId, Long voucherId, Long userId, String mode) {
        try {
            Long executed = stringRedisTemplate.execute(
                    ROLLBACK_SCRIPT,
                    Arrays.asList(),   // 脚本内自行拼接 key，保持与原版调用风格一致
                    String.valueOf(orderId),
                    String.valueOf(voucherId),
                    String.valueOf(userId),
                    mode
            );
            // 库存回来了，本机不能再认为它售罄。
            // 其他实例的标记最多再存 30 秒（设计取舍，见 LocalSoldOutGuard 注释）
            if (MODE_FULL.equals(mode)) {
                localSoldOutGuard.clear(voucherId);
            }
            if (executed != null && executed == 0L) {
                log.info("[库存回补-跳过] orderId={} 已回补过，幂等守卫生效", orderId);
                metrics.stockRollback(mode, "skipped");
                return false;
            }
            log.warn("[库存回补] orderId={} voucherId={} userId={} mode={}", orderId, voucherId, userId, mode);
            metrics.stockRollback(mode, "executed");
            return true;
        } catch (Exception e) {
            // 回补失败不影响关单结果：数据库已是终态，Redis 差异交由对账任务修正
            log.error("[库存回补失败] orderId={} voucherId={} userId={} mode={}，等待对账修复",
                    orderId, voucherId, userId, mode, e);
            metrics.stockRollback(mode, "failed");
            return false;
        }
    }
}
