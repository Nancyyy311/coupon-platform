-- ============================================================
-- 回滚脚本：把预扣的库存还回去（高可用）
-- ARGV[1] = orderId   ARGV[2] = voucherId   ARGV[3] = userId   ARGV[4] = mode
--
-- mode 两种取值，区别只在"占位要不要释放"：
--   full  ：用户最终没拿到券（发送失败 / 死信 / 超时关单）
--           -> 库存 +1，占位移除，用户下次还能抢
--   stock ：用户其实已经有有效订单（唯一索引拦下的重复下单）
--           -> 只把这次多扣的库存还回去，占位必须保留，否则他会一直抢、一直被拦
--
-- 为什么要幂等守卫（这段是后来补的，第一版漏了）：
--   RabbitMQ 是 at-least-once，同一条消息可能被投递两次；
--   producer 的 confirm-nack 与 send 抛异常也可能同时触发。
--   如果回补不做去重，库存会被还两次——这不是"多了一点库存"，
--   而是直接制造超卖，比少卖严重得多。
--
--   用 orderId 做幂等键，而不是用"占位是否存在"判断：
--   占位是 voucher+user 维度，用户退单后重新抢会复用同一个占位，
--   用它判断会把新订单的回补误判成重复。
--   订单号全局唯一且一次性使用，天然适合当幂等键。
-- ============================================================
local orderId   = ARGV[1]
local voucherId = ARGV[2]
local userId    = ARGV[3]
local mode      = ARGV[4] or 'full'

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId
local guardKey = 'seckill:rollback:' .. orderId

-- SET NX 是原子的：抢到守卫的那一次才执行回补，其余全部直接返回 0
if redis.call('set', guardKey, '1', 'NX', 'EX', 86400) == false then
    return 0
end

-- 库存 key 可能已过期（活动结束），此时不必回补，但占位仍要按 mode 处理
if redis.call('exists', stockKey) == 1 then
    redis.call('incrby', stockKey, 1)
end

if mode == 'full' then
    -- 这一句是"退单后能再买"的关键
    redis.call('srem', orderKey, userId)
end

return 1
