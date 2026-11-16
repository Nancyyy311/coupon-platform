-- ============================================================
-- 秒杀预扣脚本（高并发核心）
-- KEYS 不使用，全部走 ARGV，与原版调用方式保持一致
-- ARGV[1] = voucherId  ARGV[2] = userId
-- 返回：
--   0 成功
--   1 库存不足
--   2 重复下单
--   3 库存未预热（Redis 里没有 stock key，通常是运营没配库存或预热失败）
--   4 活动未开始
--   5 活动已结束
-- ============================================================
local voucherId = ARGV[1]
local userId    = ARGV[2]

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId
local timeKey  = 'seckill:time:'  .. voucherId

-- 0. 活动时间校验
--    时间窗口在预热时和库存一起写进 Redis，这里用 Redis 服务端时间判断，
--    既不依赖应用机器时钟，也不产生任何数据库回查。
--    timeKey 不存在时放行：兼容改造前已预热、但没有时间窗口的老券。
local beginTs = redis.call('hget', timeKey, 'begin')
local endTs   = redis.call('hget', timeKey, 'end')
if beginTs and endTs then
    local now   = redis.call('time')                                  -- {秒, 微秒}
    local nowMs = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
    if nowMs < tonumber(beginTs) then
        return 4
    end
    if nowMs > tonumber(endTs) then
        return 5
    end
end

-- 1. 库存必须存在。原版直接 tonumber(nil) 会抛错，这里显式区分"未预热"与"售罄"
local stock = tonumber(redis.call('get', stockKey))
if stock == nil then
    return 3
end

-- 2. 库存判断
if stock <= 0 then
    return 1
end

-- 3. 一人一单（Set 去重，是快路径，不是最终防线）
if redis.call('sismember', orderKey, userId) == 1 then
    return 2
end

-- 4. 扣减 + 占位。两句之间不会被打断，这是"把锁去掉"的依据
redis.call('incrby', stockKey, -1)
redis.call('sadd', orderKey, userId)

return 0
