package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.exception.ParamException;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.ParamValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_TIME_KEY;

/**
 * 优惠券服务。
 *
 * 相对原版只改了两处，都在 addSeckillVoucher 里，原因见方法内注释：
 *   1. 写入 init_stock（对账基准）。不写的话，对账任务会把这张券跳过，
 *      因为它无法回答"库存去哪了"——没有初始值就只能靠猜。
 *   2. 创建券时清空一人一单占位 Set。券 id 复用时，旧的占位会让老用户抢不了。
 *
 * 其余方法保持原样，一个字没动。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    /** 与 seckill.lua 中的 orderKey 保持完全一致：'seckill:order:' .. voucherId */
    private static final String SECKILL_ORDER_KEY = "seckill:order:";

    private final ISeckillVoucherService seckillVoucherService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        return Result.ok(vouchers);
    }

    /**
     * 覆盖 MyBatis-Plus 的 save，在落库前校验。
     *
     * 覆盖而不是在 controller 里加：普通券走 controller 的 save，
     * 秒杀券走 addSeckillVoucher 内部的 save，两条路径共用这一个入口。
     * 只拦 controller 的话，秒杀券那条路仍然能把 300 字的标题送进数据库。
     */
    @Override
    public boolean save(Voucher voucher) {
        validateForWrite(voucher);
        return super.save(voucher);
    }

    /**
     * 券的字段校验，口径对齐 tb_voucher：
     *   title varchar(255) NOT NULL、sub_title varchar(255)、rules varchar(1024)、
     *   pay_value / actual_value 都是 unsigned bigint。
     */
    private static void validateForWrite(Voucher voucher) {
        if (voucher == null) {
            throw new ParamException("优惠券数据不能为空");
        }
        ParamValidator.requireText(voucher.getTitle(), 255, "券标题");
        ParamValidator.optionalText(voucher.getSubTitle(), 255, "券副标题");
        ParamValidator.optionalText(voucher.getRules(), 1024, "使用规则");
        ParamValidator.requirePresent(voucher.getPayValue(), "支付金额");
        ParamValidator.requirePresent(voucher.getActualValue(), "抵扣金额");
        ParamValidator.notNegative(voucher.getPayValue(), "支付金额");
        ParamValidator.notNegative(voucher.getActualValue(), "抵扣金额");
    }

    /**
     * 秒杀券额外要校验的三项。
     *
     * 抽成静态方法是为了能被单测直接覆盖：走完整条 addSeckillVoucher 需要
     * 券表、秒杀表、Redis 三套桩，而真正想断言的只有"这几个入参到底放不放行"。
     *
     * 不校验的后果，实测过两条：
     *   不传 beginTime    -> toEpochMilli 抛 NPE -> "服务器异常"
     *   结束时间早于开始  -> 券建出来了，但活动窗口是负的，谁都抢不到，
     *                        而且没有任何报错，只能靠人肉发现
     */
    static void validateSeckillWindow(Voucher voucher) {
        ParamValidator.requirePresent(voucher.getStock(), "秒杀库存");
        ParamValidator.notNegative(voucher.getStock(), "秒杀库存");
        ParamValidator.requirePresent(voucher.getBeginTime(), "活动开始时间");
        ParamValidator.requirePresent(voucher.getEndTime(), "活动结束时间");
        if (!voucher.getEndTime().isAfter(voucher.getBeginTime())) {
            throw new ParamException("活动结束时间必须晚于开始时间");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSeckillVoucher(Voucher voucher) {
        // 0. 先校验活动窗口，再动库：校验失败时事务里一条 SQL 都还没发
        validateSeckillWindow(voucher);

        // 1. 保存优惠券
        save(voucher);

        // 2. 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);

        // 3. 记录初始库存，作为对账基准（init_stock 由 V1__upgrade.sql 新增）
        //    没有这一列时，对账只知道 Redis 扣了多少，不知道"本来应该有多少"，
        //    也就无法判断"少卖"和"超卖"哪个方向出了问题。
        seckillVoucherService.update()
                .setSql("init_stock = stock")
                .eq("voucher_id", voucher.getId())
                .update();

        // 4. 库存预热到 Redis
        stringRedisTemplate.opsForValue()
                .set(SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());

        // 4.1 活动时间窗口一起预热
        //     让 seckill.lua 用 redis.call('time') 直接判断活动是否开始/结束，
        //     下单路径因此不需要为了校验时间回查数据库，也不会受应用机器时钟影响。
        Map<String, String> timeWindow = new HashMap<>(2);
        timeWindow.put("begin", String.valueOf(toEpochMilli(voucher.getBeginTime())));
        timeWindow.put("end", String.valueOf(toEpochMilli(voucher.getEndTime())));
        stringRedisTemplate.opsForHash().putAll(SECKILL_TIME_KEY + voucher.getId(), timeWindow);

        // 5. 清掉可能残留的一人一单占位。
        //    券 id 若被复用（测试环境常见），旧的 Set 会让老用户直接提示"每人限购一张"，
        //    而实际上他根本没抢过这张新券。
        stringRedisTemplate.delete(SECKILL_ORDER_KEY + voucher.getId());

        log.info("[新建秒杀券] voucherId={} stock={}，库存与活动时间已预热 Redis，占位 Set 已清空",
                voucher.getId(), voucher.getStock());
    }

    /**
     * LocalDateTime -> epoch 毫秒（按服务器默认时区）。
     * 与 seckill.lua 中 redis.call('time') 的毫秒口径保持一致。
     */
    private static long toEpochMilli(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
