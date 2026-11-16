package com.hmdp.job;

import com.hmdp.utils.JobLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 库存与订单对账（高可用，先发现再修复）。
 *
 * 为什么必须有这个任务：
 *   只要"Redis 预扣"和"数据库落库"是两条链路，它们之间就一定有窗口。
 *   消息可能丢、消费者可能挂、进程可能正好在发送后被 kill。
 *   没有对账的话，这些差异会一直存在且无人知晓——库存显示有，但用户抢不到，
 *   或者库存显示没有，实际还有。这类问题用户不会报错，只会流失。
 *
 * 三个核对口径：
 *   ① Redis 自洽：initStock - redisStock 应等于 Redis 里占位用户数
 *   ② 幽灵扣减：Redis 有占位，数据库没有订单（抢到了但没落库）
 *   ③ 占位丢失：数据库有订单，Redis 没有占位（Redis 重启/主从切换丢数据）
 *
 * 为什么 ② 只告警不自动修，③ 可以自动修：
 *   ② 自动修 = 把库存加回去。但如果那条消息只是还在队列里排队，之后又被消费成功，
 *      就会变成"库存加回去了，订单也建了"——直接超卖。所以必须先确认队列已空、
 *      消息确实丢了，才能人工执行修复。
 *   ③ 自动修 = 把占位补回 Set。这个动作只会让用户不能再买，不会让可售库存变多，
 *      不存在超卖风险，可以安全地自动执行。
 *   ——对账的价值首先在于"发现"，不在于"全自动修复"。分不清修复是否安全的场景，
 *     宁可告警，也不要自作聪明。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.reconcile-job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StockReconcileJob {

    /**
     * 有效占用库存的订单状态：1 未支付、2 已支付、3 已核销、5 退款中。
     *
     * <p>本项目未做支付，实际只会命中 1；2/3/5 是预留——将来接支付时，
     * 对账口径不用改（否则已支付的单会被当成"不该占库存"，把库存虚增回去）。
     */
    private static final String VALID_STATUS = "1,2,3,5";

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String ORDER_KEY_PREFIX = "seckill:order:";

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final JobLock jobLock;

    private static final String JOB_NAME = "stock-reconcile";

    /**
     * 锁 TTL 取一个调度间隔（默认 5 分钟）。
     * 正常跑完就释放，TTL 只在进程被 kill 时起作用——最坏是漏跑一轮，下一轮自然接上。
     */
    private static final long LOCK_TTL_SECONDS = 300L;

    /** 默认 5 分钟一轮，可在配置里覆盖 */
    @Scheduled(fixedDelayString = "${app.reconcile.interval-ms:300000}", initialDelay = 120_000)
    public void reconcile() {
        // 两个实例同时对账不会改坏数据（③ 的修复只是 SADD，幂等），
        // 但会各扫一遍全表、各报一遍同样的告警，日志里同一个问题出现两次反而干扰判断
        String token = jobLock.tryAcquire(JOB_NAME, LOCK_TTL_SECONDS);
        if (token == null) {
            return;
        }
        try {
            doReconcile();
        } finally {
            jobLock.release(JOB_NAME, token);
        }
    }

    private void doReconcile() {
        List<Map<String, Object>> vouchers;
        try {
            vouchers = jdbcTemplate.queryForList(
                    "SELECT voucher_id, stock, init_stock FROM tb_seckill_voucher WHERE init_stock > 0");
        } catch (Exception e) {
            log.error("[对账] 读取券列表失败", e);
            return;
        }

        for (Map<String, Object> row : vouchers) {
            Long voucherId = ((Number) row.get("voucher_id")).longValue();
            long initStock = ((Number) row.get("init_stock")).longValue();
            try {
                reconcileOne(voucherId, initStock);
            } catch (Exception e) {
                log.error("[对账] voucherId={} 处理异常", voucherId, e);
            }
        }
    }

    private void reconcileOne(Long voucherId, long initStock) {
        String stockValue = stringRedisTemplate.opsForValue().get(STOCK_KEY_PREFIX + voucherId);
        if (stockValue == null) {
            // 券未预热到 Redis，不参与对账
            return;
        }
        long redisStock = Long.parseLong(stockValue);
        Set<Long> holders = scanHolders(ORDER_KEY_PREFIX + voucherId);
        Set<Long> dbUsers = queryOrderUsers(voucherId);

        // ① Redis 自身是否自洽
        long consumedInRedis = initStock - redisStock;
        if (consumedInRedis != holders.size()) {
            log.error("[对账-异常] voucherId={} Redis 账目矛盾：已扣 {} 但占位用户 {} 个",
                    voucherId, consumedInRedis, holders.size());
        }

        // ② 幽灵扣减：Redis 占位了但数据库没单
        Set<Long> orphan = new LinkedHashSet<>(holders);
        orphan.removeAll(dbUsers);
        if (!orphan.isEmpty()) {
            log.error("[对账-需人工] voucherId={} 发现 {} 个幽灵占位（Redis 有占位、DB 无订单），"
                            + "请先确认 MQ 与死信队列已清空，再执行修复。userId 示例={}",
                    voucherId, orphan.size(), new ArrayList<>(orphan).subList(0, Math.min(5, orphan.size())));
        }

        // ③ 占位丢失：数据库有单但 Redis 没占位。补回去是安全的，不会让库存变多
        Set<Long> missing = new LinkedHashSet<>(dbUsers);
        missing.removeAll(holders);
        if (!missing.isEmpty()) {
            log.warn("[对账-自动修复] voucherId={} 有 {} 个订单在 Redis 中缺少占位，正在补回", voucherId, missing.size());
            for (Long userId : missing) {
                stringRedisTemplate.opsForSet().add(ORDER_KEY_PREFIX + voucherId, String.valueOf(userId));
            }
        }

        // 最终账目：初始库存 = 剩余库存 + 有效订单数
        long diff = initStock - redisStock - dbUsers.size();
        if (diff != 0) {
            log.error("[对账-账目不平] voucherId={} initStock={} redisStock={} dbOrders={} diff={}",
                    voucherId, initStock, redisStock, dbUsers.size(), diff);
        }
    }

    /** 用 SSCAN 分批遍历，避免 SMEMBERS 在大 Set 上阻塞 Redis 主线程 */
    private Set<Long> scanHolders(String key) {
        Set<Long> result = new HashSet<>();
        Cursor<String> cursor = null;
        try {
            cursor = stringRedisTemplate.opsForSet()
                    .scan(key, ScanOptions.scanOptions().count(500).build());
            while (cursor.hasNext()) {
                try {
                    result.add(Long.valueOf(cursor.next()));
                } catch (NumberFormatException ignored) {
                    // 非用户 id 的脏数据跳过，不影响其他成员
                }
            }
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    private Set<Long> queryOrderUsers(Long voucherId) {
        List<Long> users = jdbcTemplate.queryForList(
                "SELECT user_id FROM tb_voucher_order WHERE voucher_id = ? AND status IN (" + VALID_STATUS + ")",
                Long.class, voucherId);
        return new HashSet<>(users);
    }
}
