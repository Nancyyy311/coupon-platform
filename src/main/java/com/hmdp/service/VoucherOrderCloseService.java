package com.hmdp.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时关单（高可用）。
 *
 * 原版没有这一段，后果是：订单落库后永远停在"未支付"（本项目没有支付入口，
 * 抢到券就是终点，不会有任何动作把它推进到已支付），
 * 而它占的那 1 个库存名额就再也回不来了。
 * 这不是性能问题，是**业务漏洞**——100 张券最后只能卖出 99 张，
 * 剩下那张被无声占死，而且没有任何日志会告诉你。
 *
 * 实现上刻意不引入延迟队列（Redisson DelayQueue / RabbitMQ 延迟插件），原因：
 *   1) 延迟队列的消息时间和真实触发时间会漂移，堆积时更明显；扫表是"到点才认账"，
 *      系统停机两小时再启动，该关的单一个都不会漏
 *   2) 扫表可以重跑、可以补跑、可以按条件临时补一批；消息发出去就只能等
 *   3) 联合索引 (status, create_time) 下，每分钟扫几百条是毫秒级
 *   如果订单量到千万级且要求秒级关单精度，再换延迟队列，那时收益才大于成本。
 *
 * 注意：closeOne 是事务方法，必须由外部类（Job）调用才会经过 Spring 代理。
 *      在同一个类里 this.closeOne() 调用同样会让事务失效——这正是原版踩过的坑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherOrderCloseService {

    /** 未支付订单的存活时间（分钟） */
    public static final int PAY_TIMEOUT_MINUTES = 15;

    /** 单次扫描上限，避免一次关太多把数据库压住 */
    private static final int BATCH_SIZE = 500;

    private final VoucherOrderMapper voucherOrderMapper;
    private final ISeckillVoucherService seckillVoucherService;

    /** 查出本批待关闭的订单，不做任何写操作 */
    public List<VoucherOrder> queryTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(PAY_TIMEOUT_MINUTES);
        QueryWrapper<VoucherOrder> query = new QueryWrapper<>();
        query.eq("status", 1)
                .lt("create_time", deadline)
                .orderByAsc("create_time")
                .last("LIMIT " + BATCH_SIZE);
        return voucherOrderMapper.selectList(query);
    }

    /**
     * 关闭单笔订单（只改数据库，事务内完成）。
     *
     * 核心是那句 "WHERE id = ? AND status = 1"：
     *   本项目没有支付入口，所以"关单把已支付订单改掉"现在不会发生；
     *   但这个条件必须留着——一旦将来接入支付，回调可能在第 14 分 59 秒把状态推到 2，
     *   关单任务在第 15 分整扫到它，无条件 UPDATE 就会变成"钱收了、券退了"。
     *   加上 status = 1 之后，先改状态的一方赢，这里影响行数为 0，
     *   说明"这笔订单已经不是我该关的了"，直接放弃。
     *
     * 影响行数 = 1 还有一个作用：它证明"这次关单是我做的"，
     * 所以后续的库存回补只会执行一次，天然幂等，不需要额外的分布式锁。
     *
     * @return true 表示本次真的关闭了这笔订单
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean closeOne(VoucherOrder order) {
        UpdateWrapper<VoucherOrder> orderUpdate = new UpdateWrapper<>();
        orderUpdate.eq("id", order.getId())
                .eq("status", 1)
                .set("status", 4) // 4 已取消
                // 关键：取消后把 active_flag 置 NULL，让唯一索引放行，
                // 用户才能重新抢这张券。不加这一句，退单的人就永远买不了了
                .set("active_flag", null)
                .set("update_time", LocalDateTime.now());

        int rows = voucherOrderMapper.update(null, orderUpdate);
        if (rows == 0) {
            // 已被支付回调或另一次关单处理过，什么都不做
            return false;
        }

        // 数据库库存回补，与状态变更同一个事务：要么都对，要么都不对
        seckillVoucherService.update()
                .setSql("stock = stock + 1")
                .eq("voucher_id", order.getVoucherId())
                .update();

        return true;
    }
}
