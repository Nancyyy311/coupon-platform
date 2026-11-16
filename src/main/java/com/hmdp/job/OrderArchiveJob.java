package com.hmdp.job;

import com.hmdp.utils.JobLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单冷热分离（替代分库分表）。
 *
 * 为什么不做分库分表：
 *   分片的前提是数据量真的到了单表撑不住的程度（千万行以上）。
 *   本项目的订单量离那个量级差着两个数量级，上 ShardingSphere 之后会立刻多出
 *   五个必须解决的问题：跨分片分页、跨分片 Join、分布式事务、扩容重分布、数据迁移回滚。
 *   每一个都能单独问 20 分钟，而收益是零——因为根本不需要。
 *   面试官只需要一句"你实际多少数据"，分库分表的故事就讲不下去了。
 *
 * 真实系统里 90% 的"订单表慢"不是数据量问题，是"历史订单和今天订单混在一张表里查"。
 * 冷热分离解决的正是这个：把 90 天前的终态订单搬到历史表，主表只留热数据，
 * 索引高度下降，查询自然变快，而且成本只有一个定时任务。
 *
 * 边界（被追问时要能说清）：
 *   - 只归档终态订单（已支付/已核销/已取消/已退款），未支付的不动
 *     （本项目未做支付，实际只会归档到"已取消"这一类）
 *   - 分批 1000 条，避免长事务和主从延迟；单次最多 100 批，防止异常情况下无限循环
 *   - 先插历史表再删主表。先删后插一旦中途失败就是丢数据，反过来最坏只是重复，
 *     且重复有 ON DUPLICATE KEY UPDATE 兜住，不会报错中断
 *
 * 多实例（这版修复的三处问题，都是双实例场景下才会暴露的）：
 *   1. **加了任务锁**。原来两个实例都在 3:00 跑，同时 SELECT 出同一批 id，
 *      后 INSERT 的那个必然主键冲突。原实现 catch 之后直接 break，
 *      表现是"一个节点归档成功、另一个节点每天报一次 ERROR 且整次归档中止"。
 *   2. **列名写死，不再 SELECT ***。原来 `INSERT INTO ... SELECT *` 依赖两张表
 *      列顺序与列数完全一致，给主表加一列（比如 active_flag）而历史表没跟上，
 *      这条 SQL 会直接失败——而且要等到凌晨 3 点才暴露。
 *   3. **冲突降级为跳过**。ON DUPLICATE KEY UPDATE 让"这批里有一条已经在历史表"
 *      不再中断整次归档，剩下的照样搬完。上一版是 break，一条冲突卡死全部。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.archive-job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderArchiveJob {

    /** 热数据保留天数 */
    private static final int HOT_DAYS = 90;

    private static final int BATCH_SIZE = 1000;

    /** 单次最多搬多少批，防止"删不掉导致 SELECT 一直返回同一批"这类异常下无限循环 */
    private static final int MAX_BATCHES = 100;

    /** 终态订单：2 已支付 / 3 已核销 / 4 已取消 / 6 已退款 */
    private static final String ARCHIVABLE_STATUS = "2,3,4,6";

    /**
     * 显式列清单。主表加列时必须同步这里，否则新列不会归档（但不报错，是"悄悄漏"),
     * 比 SELECT * 的"凌晨三点突然列数不匹配"更可控。
     */
    private static final String COLUMNS = "id, user_id, voucher_id, pay_type, status, create_time,"
            + " pay_time, use_time, refund_time, update_time, active_flag";

    private static final String JOB_NAME = "order-archive";

    /** 锁 TTL：正常耗时是秒级，给 30 分钟足够；崩溃时最坏等 30 分钟恢复，下一个 3:00 照常 */
    private static final long LOCK_TTL_SECONDS = 30 * 60L;

    private final JdbcTemplate jdbcTemplate;
    private final JobLock jobLock;

    /**
     * 每天凌晨 3 点执行，避开业务高峰。
     *
     * cron 从配置读，默认仍是 3:00。为什么不在代码里写死：
     * 这个任务一天只跑一次，想验证或演示就得等到凌晨。改成可配置之后，
     * 起实例时传 {@code --app.archive-job.cron=0/20 * * * * ?} 就能每 20 秒跑一轮，
     * 用来验证多实例互斥和冲突处理。生产环境不配就是默认值，行为不变。
     */
    @Scheduled(cron = "${app.archive-job.cron:0 0 3 * * ?}")
    public void archive() {
        // 返回 null = 其他实例正在跑；返回空串 = Redis 不可用，降级照常执行（本任务不依赖 Redis）
        String token = jobLock.tryAcquire(JOB_NAME, LOCK_TTL_SECONDS);
        if (token == null) {
            log.debug("[订单归档] 其他实例正在执行，本次跳过");
            return;
        }
        try {
            doArchive();
        } finally {
            jobLock.release(JOB_NAME, token);
        }
    }

    private void doArchive() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(HOT_DAYS);
        int total = 0;

        for (int batch = 0; batch < MAX_BATCHES; batch++) {
            List<Long> ids;
            try {
                ids = jdbcTemplate.queryForList(
                        "SELECT id FROM tb_voucher_order "
                                + "WHERE status IN (" + ARCHIVABLE_STATUS + ") AND create_time < ? "
                                + "ORDER BY create_time LIMIT " + BATCH_SIZE,
                        Long.class, deadline);
            } catch (Exception e) {
                // 查询也要兜住：数据库不可用时如果不接住，异常会抛给调度线程。
                // 单看效果只是"这一轮没归档"，但日志里是一条没有上下文的堆栈，
                // 而且会掩盖"是查询失败还是写入失败"这个关键区别。
                log.error("[订单归档] 查询待归档订单失败，已中止本次归档", e);
                break;
            }

            if (ids.isEmpty()) {
                break;
            }

            // 拼 "1,2,3" 这种 id 列表，用 StringBuilder 直接拼：
            // Stream + Collectors.joining 在这里省不了代码，还要多一个 Stream 分发包的中间对象。
            StringBuilder sb = new StringBuilder(ids.size() * 20);
            for (int i = 0; i < ids.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(ids.get(i));
            }
            String idList = sb.toString();

            try {
                // ON DUPLICATE KEY UPDATE 这里是"什么都不改"的空操作：
                // 已经归档过的行直接跳过，不像裸 INSERT 那样抛主键冲突把整批打断。
                //
                // 为什么两个 id 都要写全表名：INSERT ... SELECT ... ON DUPLICATE KEY UPDATE
                // 里直接写 `id = id` 会报 "Column 'id' in field list is ambiguous"——
                // MySQL 分不清是目标表的 id 还是 SELECT 源表的 id。这不是理论问题，
                // 是这段代码第一版真炸出来的错，而它只会在凌晨 3 点执行时才暴露。
                //
                // 不用 INSERT IGNORE：那会把"字段超长被截断"这类错误一起吞掉，
                // 这里只想忽略主键冲突这一种。
                jdbcTemplate.update("INSERT INTO tb_voucher_order_history (" + COLUMNS + ") "
                        + "SELECT " + COLUMNS + " FROM tb_voucher_order WHERE id IN (" + idList + ") "
                        + "ON DUPLICATE KEY UPDATE tb_voucher_order_history.id = tb_voucher_order_history.id");

                // 以主表实际删除的行数计数：插入可能因冲突全部跳过，用 ids.size() 会虚高
                int deleted = jdbcTemplate.update(
                        "DELETE FROM tb_voucher_order WHERE id IN (" + idList + ")");
                total += deleted;
            } catch (Exception e) {
                log.error("[订单归档] 批次失败，已中止本次归档。"
                        + "若是历史表缺失或列不匹配，需先执行 db/V1__upgrade.sql", e);
                break;
            }
        }

        if (total > 0) {
            log.warn("[订单归档] 本次归档 {} 笔 {} 天前的终态订单", total, HOT_DAYS);
        }
    }
}
