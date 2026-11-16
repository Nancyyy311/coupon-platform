package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.cache.LocalSoldOutGuard;
import com.hmdp.dto.Result;
import com.hmdp.dto.SeckillResultDTO;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.monitor.SeckillMetrics;
import com.hmdp.mq.OrderMessage;
import com.hmdp.mq.OrderProducer;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.VoucherOrderTxService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SnowflakeIdWorker;
import com.hmdp.utils.TokenBucketRateLimiter;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.Collections;

/**
 * 秒杀下单（高并发主链路）。
 *
 * 与原版的差异，逐条对应改造目标：
 * ------------------------------------------------------------------
 * 1. 本地售罄标识        新增。券卖完后 30s 内的请求不进 Redis      [高并发]
 * 2. Lua 原子预扣        保留并修复。库存+一人一单一次完成          [高并发]
 * 3. 去掉 Redisson 锁    删除。锁保护的东西已被 Lua 原子性覆盖      [高并发]
 * 4. Stream 换 RabbitMQ  替换。获得 confirm / 死信 / 并发消费       [高可用]
 * 5. 唯一索引兜底        新增（见 V1__upgrade.sql + TxService）     [高可用]
 * 6. 订单号换雪花        替换。不暴露业务量，省一次 Redis 往返      [高性能]
 * 7. 下单不发 SQL        保持。整个接口只碰 Redis + MQ              [高性能]
 *
 * 关于"为什么把锁删掉"（这是最容易被追问的一处，答案要能一句话说清）：
 *   原来的锁保护的是"查库存 → 扣库存"这段临界区。现在这段逻辑整体在 Lua 里执行，
 *   Redis 单线程保证它不会被打断，再加上数据库 uk_user_voucher 唯一索引兜底，
 *   锁要保护的东西已经不存在了。留着锁反而引入三个新故障点：
 *   锁超时提前释放、续期失败、误删他人锁。
 *   判断依据：锁是用来补原子性缺口的，原子性缺口没了，锁就是纯负担。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder>
        implements IVoucherOrderService {

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        // 文件名小写，Linux 区分大小写，原版写的是 seckill.Lua
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /** Lua 返回码 */
    private static final int CODE_SUCCESS = 0;
    private static final int CODE_SOLD_OUT = 1;
    private static final int CODE_DUPLICATE = 2;
    /** 库存 key 不存在，属于配置/预热问题，不是业务上"活动没开始" */
    private static final int CODE_NOT_READY = 3;
    private static final int CODE_NOT_START = 4;
    private static final int CODE_ENDED = 5;

    /** 订单状态"已取消"。超时关单任务把 1 改成 4 */
    private static final int ORDER_STATUS_CANCELLED = 4;

    /** 处理中时建议前端多久后再查（毫秒）。正常链路消费者在几十毫秒内就落库了 */
    private static final long PENDING_RETRY_MS = 500L;

    /**
     * PENDING 的等待上限（秒）。超过它仍然查不到订单、也没有回滚标记，就判定消息已丢。
     *
     * 取值依据：正常链路消费者 50ms 内落库，60 秒是 1000 倍余量；
     * 只有积压到"一分钟还没消费完"才可能误判，那种情况下让用户重试也比让他无限等待好。
     * 可用 app.seckill.pending-timeout-seconds 覆盖。
     */
    @Value("${app.seckill.pending-timeout-seconds:60}")
    private long pendingTimeoutSeconds = 60;

    private final StringRedisTemplate stringRedisTemplate;
    private final SnowflakeIdWorker snowflakeIdWorker;
    private final OrderProducer orderProducer;
    private final LocalSoldOutGuard localSoldOutGuard;
    private final VoucherOrderTxService voucherOrderTxService;
    private final TokenBucketRateLimiter rateLimiter;
    private final SeckillMetrics metrics;

    @PostConstruct
    public void init() {
        // 原版在启动时创建 Stream 消费组，换 MQ 后这一步已不需要，
        // 但保留一个启动日志，方便确认脚本文件路径正确、能被打包进 jar
        log.info("[秒杀服务] Lua 脚本加载完成，下单链路：本地售罄标识 -> Redis 原子预扣 -> RabbitMQ");
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();

        // 第一层：本机售罄标识。命中则零 Redis 命令返回，挡住卖完后的大量无效点击
        if (localSoldOutGuard.isSoldOut(voucherId)) {
            metrics.localSoldOutBlock();
            metrics.seckillResult(SeckillMetrics.RESULT_SOLD_OUT);
            return Result.fail("已售罄");
        }

        // 第二层：按用户限流（本地零 IO 粗筛 + Redis 令牌桶精筛）。脚本刷单和正常点击的区别在频率，不在身份。
        // 排在售罄判断之后有两个原因：
        //   ① 已经卖完的券没必要再消耗一次 Redis（限流本身也是一次脚本调用）；
        //   ② 卖完时的正确文案是"已售罄"，不该让用户看到"手速太快"这种误导性提示。
        TokenBucketRateLimiter.Decision decision = rateLimiter.acquire("seckill", userId);
        if (decision != TokenBucketRateLimiter.Decision.ALLOWED) {
            metrics.seckillResult(decision == TokenBucketRateLimiter.Decision.LIMITED_LOCAL
                    ? SeckillMetrics.RESULT_RATE_LIMITED_LOCAL
                    : SeckillMetrics.RESULT_RATE_LIMITED);
            return Result.fail("手速太快了，请稍后再试");
        }

        // 第三层：Redis 原子预扣。库存判断 + 一人一单 + 扣减 + 占位，一次执行完
        long orderId = snowflakeIdWorker.nextId();
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                String.valueOf(voucherId),
                String.valueOf(userId)
        );
        int code = result == null ? -1 : result.intValue();

        if (code != CODE_SUCCESS) {
            return handleFailCode(code, voucherId);
        }

        // 第四层：投递消息。发送失败会同步回滚 Redis 预扣，不会留下幽灵库存
        OrderMessage message = new OrderMessage(orderId, userId, voucherId, System.currentTimeMillis());
        if (!orderProducer.send(message)) {
            metrics.seckillResult(SeckillMetrics.RESULT_ERROR);
            return Result.fail("系统繁忙，请稍后重试");
        }

        // 立刻返回订单号，前端凭它查询结果。落库由消费者异步完成
        metrics.seckillResult(SeckillMetrics.RESULT_SUCCESS);
        return Result.ok(orderId);
    }

    private Result handleFailCode(int code, Long voucherId) {
        switch (code) {
            case CODE_SOLD_OUT:
                // 只有真的卖完才打本地标记；未开始、未预热不能打，否则会把正常活动误判成售罄
                localSoldOutGuard.markSoldOut(voucherId);
                metrics.seckillResult(SeckillMetrics.RESULT_SOLD_OUT);
                return Result.fail("已售罄");
            case CODE_DUPLICATE:
                metrics.seckillResult(SeckillMetrics.RESULT_DUPLICATE);
                return Result.fail("每人限购一张");
            case CODE_NOT_READY:
                // 语义是"Redis 里没有库存 key"，属于配置/预热问题，
                // 不能再说成"活动未开始"，否则排查时会被带到错误方向
                log.warn("[秒杀] 库存未预热 voucherId={}，请检查预热流程", voucherId);
                metrics.seckillResult(SeckillMetrics.RESULT_NOT_READY);
                return Result.fail("活动太火爆了，请稍后再试");
            case CODE_NOT_START:
                metrics.seckillResult(SeckillMetrics.RESULT_NOT_START);
                return Result.fail("活动未开始");
            case CODE_ENDED:
                metrics.seckillResult(SeckillMetrics.RESULT_ENDED);
                return Result.fail("活动已结束");
            default:
                log.error("[秒杀脚本异常] voucherId={} code={}", voucherId, code);
                metrics.seckillResult(SeckillMetrics.RESULT_ERROR);
                return Result.fail("系统繁忙，请稍后重试");
        }
    }

    /**
     * 原接口 IVoucherOrderService 声明的方法，必须实现，否则编译不过。
     *
     * 现在落库统一由 MQ 消费者走 VoucherOrderTxService，这个方法只是一层兼容：
     * 如果还有别处（比如同步下单的旧入口）调用它，行为与消费者完全一致——
     * 幂等、条件扣库存、唯一索引兜底，不会因为走了另一条路径就绕过事务。
     */
    @Override
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        long createTime = voucherOrder.getCreateTime() == null
                ? System.currentTimeMillis()
                : voucherOrder.getCreateTime()
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        voucherOrderTxService.createOrder(new OrderMessage(
                voucherOrder.getId(),
                voucherOrder.getUserId(),
                voucherOrder.getVoucherId(),
                createTime
        ));
    }

    /**
     * 查询秒杀结果。
     *
     * 设计上只做一次主键查询 + 一次 Redis 存在性判断，不轮询 MQ、不查队列长度：
     *   "消息还在不在队列里"这个信息对用户没有意义，用户只关心"我的单成没成"。
     *   数据库是唯一真相，Redis 的回滚标记只用来区分"还在处理"和"确实失败了"。
     *
     * 为什么不用「查订单表 → 没有就返回失败」：
     *   异步落库有窗口期。刚提交完 200ms 去查，数据库必然还没有单，
     *   此时返回失败是错的——用户会以为没抢到，然后再点一次，
     *   结果被"一人一单"挡住，体验上就是"我明明抢到了却说我没抢到"。
     *
     * 越权：订单号虽然是雪花、不可枚举，但仍然要校验归属。
     *   "猜不到"不是权限模型，URL 一旦被分享或日志泄露就等于越权读。
     */
    @Override
    public Result querySeckillResult(Long orderId) {
        Long userId = UserHolder.getUser().getId();

        VoucherOrder order = getById(orderId);
        if (order != null) {
            if (!userId.equals(order.getUserId())) {
                metrics.seckillResultQuery("forbidden");
                log.warn("[查询秒杀结果-越权] orderId={} 查询人={} 归属人={}", orderId, userId, order.getUserId());
                return Result.fail("无权查看该订单");
            }

            boolean cancelled = order.getStatus() != null && order.getStatus() == ORDER_STATUS_CANCELLED;
            String state = cancelled ? SeckillResultDTO.STATE_CLOSED : SeckillResultDTO.STATE_CREATED;
            metrics.seckillResultQuery(state);

            return Result.ok(new SeckillResultDTO()
                    .setOrderId(order.getId())
                    .setState(state)
                    .setOrderStatus(order.getStatus())
                    .setCreateTime(order.getCreateTime())
                    .setMsg(cancelled ? "订单已超时取消" : "下单成功"));
        }

        // 数据库没有单，看 Redis 有没有这笔订单号的回滚标记
        boolean rolledBack = false;
        try {
            rolledBack = Boolean.TRUE.equals(
                    stringRedisTemplate.hasKey(RedisConstants.SECKILL_ROLLBACK_KEY + orderId));
        } catch (Exception e) {
            // Redis 抖动时不能把"不知道"说成"失败"，否则用户会放弃一个其实还在处理中的订单
            log.warn("[查询秒杀结果] 读取回滚标记失败 orderId={}，按处理中返回", orderId, e);
        }

        if (rolledBack) {
            metrics.seckillResultQuery(SeckillResultDTO.STATE_FAILED);
            return Result.ok(new SeckillResultDTO()
                    .setOrderId(orderId)
                    .setState(SeckillResultDTO.STATE_FAILED)
                    .setMsg("未抢到，库存已退回"));
        }

        // 既没有单、也没有回滚标记，只剩两种可能：消息还在路上，或者消息已经丢了。
        // 订单号里就带着生成时间（雪花高位是时间戳），用它判断"已经等了多久"，
        // 不需要额外存一条 pending 标记到 Redis —— 那会在下单热路径上多一次写。
        // 超过阈值就给出终态，否则 PENDING 会变成唯一一个没有出口的状态。
        long ageMs = SnowflakeIdWorker.ageMillis(orderId, System.currentTimeMillis());
        if (orderId > 0 && ageMs > pendingTimeoutSeconds * 1000L) {
            metrics.seckillResultQuery(SeckillResultDTO.STATE_TIMEOUT);
            log.warn("[查询秒杀结果-超时] orderId={} 已等待 {}ms 仍无订单且无回滚标记，判定消息丢失", orderId, ageMs);
            return Result.ok(new SeckillResultDTO()
                    .setOrderId(orderId)
                    .setState(SeckillResultDTO.STATE_TIMEOUT)
                    .setMsg("处理超时，请重新下单"));
        }

        metrics.seckillResultQuery(SeckillResultDTO.STATE_PENDING);
        return Result.ok(new SeckillResultDTO()
                .setOrderId(orderId)
                .setState(SeckillResultDTO.STATE_PENDING)
                .setRetryAfterMs(PENDING_RETRY_MS)
                .setMsg("正在处理中"));
    }
}
