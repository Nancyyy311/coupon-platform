package com.hmdp.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 雪花 ID：订单号生成器。
 *
 * 为什么不用原版的 Redis 自增（icr:order:yyyy:MM:dd）：
 *   1) 号是连续的，谁都能读出今天卖了多少单——两个账号隔周各下一单，相减就是日订单量；
 *   2) 每次下单多一次 Redis 往返，秒杀链路上这次往返完全可以省掉。
 * 雪花是趋势递增（对 B+Tree 插入友好）但不连续，两个问题一起解决。
 *
 * workerId 从哪来：不写死在代码里，由部署层注入（app.snowflake.worker-id=${WORKER_ID:0}），
 * 单机默认 0，多副本时每个副本给不同的值。启动时校验 0~31，越界直接启动失败——
 * 把"配错了"暴露在第一秒，而不是变成运行中偶发、无法稳定复现的主键冲突。
 *
 * 时钟回拨怎么办（见 nextId）：5ms 以内等它追上，超过就拒绝发号。
 * 宁可这一瞬间下单失败，也不产生重复订单号。
 */
@Slf4j
@Component
public class SnowflakeIdWorker {

    /** 起始纪元：2024-01-01 00:00:00 UTC */
    private static final long EPOCH = 1704067200000L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    /**
     * 机器编号，由部署层注入：单机默认 0，多副本时每个副本给不同值。
     * 越界会在启动时被 check() 拦住。
     */
    @Value("${app.snowflake.worker-id:0}")
    private long workerId;

    @Value("${app.snowflake.datacenter-id:0}")
    private long datacenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    @PostConstruct
    public void check() {
        validateWorkerId(workerId);
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException("datacenterId 必须在 0~" + MAX_DATACENTER_ID + " 之间");
        }
        // 打出来是为了运维能在启动日志里核对：多副本部署时互不相同才算配对
        log.info("[雪花] workerId={} datacenterId={}", workerId, datacenterId);
    }

    private void validateWorkerId(long id) {
        if (id < 0 || id > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId 必须在 0~" + MAX_WORKER_ID + " 之间");
        }
    }

    /**
     * 从订单号反解生成时间（毫秒）。
     *
     * 用途：结果查询时判断"这笔单已经等了多久"，不需要额外存一条"下单时间"到 Redis。
     * 雪花的高位就是时间戳，解码是纯位运算，零 IO —— 这是异步链路里少有的免费信息。
     *
     * 代价：这段逻辑依赖发号格式（时间戳在第 22 位以上）。
     * 如果以后换发号器（号段、UUID），这里必须跟着改，否则会把"查不到的单"误判成超时。
     * 所以放在 SnowflakeIdWorker 里，而不是散落在业务代码中。
     *
     * @return 生成时间；对于不是雪花格式的 id（比如手工乱填），返回的是一个无意义的历史时间，
     *         调用方按"很旧"处理即可，那正是希望的结果
     */
    public static long timestampOf(long id) {
        return (id >>> TIMESTAMP_SHIFT) + EPOCH;
    }

    /** 该订单号已经生成了多久（毫秒）。时钟回拨导致为负时调用方应按 0 处理 */
    public static long ageMillis(long id, long nowMillis) {
        return nowMillis - timestampOf(id);
    }

    public long getWorkerId() {
        return workerId;
    }

    public long getDatacenterId() {
        return datacenterId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        // 时钟回拨：本地机器对时、虚拟机迁移都可能让时间往回跳
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                // 小幅回拨：等它追上，不动用异常。
                // 这里用 sleep 而不是 wait：nextId 是 synchronized 方法，
                // wait 会释放监视器锁，让其他线程一起进来重复等待，把发号顺序搞乱；
                // sleep 持锁等待，正好实现了"回拨期间暂停发号"的语义
                try {
                    Thread.sleep(offset + 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("生成订单号被中断", e);
                }
                timestamp = System.currentTimeMillis();
                if (timestamp < lastTimestamp) {
                    throw new IllegalStateException("时钟回拨超过阈值，拒绝发号，避免产生重复订单号");
                }
            } else {
                throw new IllegalStateException("时钟回拨 " + offset + "ms，拒绝发号");
            }
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 同毫秒内序列用尽，等到下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
