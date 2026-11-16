-- 令牌桶限流（按用户 + 场景维度）
--
-- 为什么用令牌桶而不是"固定窗口计数"：
--   固定窗口在窗口切换的瞬间允许 2 倍流量通过（第 59 秒打满 + 第 61 秒再打满），
--   秒杀场景下这正好是最危险的时刻。令牌桶允许"突发 = 桶容量"，但长期速率被
--   恒定补充的速率锁死，不会出现双倍窗口。
--
-- 为什么时间用 redis.call('time') 而不是应用传进来：
--   多实例部署时各机器时钟可能有几十毫秒偏差，用本地时间会出现
--   "A 机器认为桶已满、B 机器认为桶是空的"，限流就不再是全局约束。
--   集群里只有 Redis 的时间是唯一的，和 seckill.lua 的时间窗校验是同一个道理。
--
-- KEYS[1] = 该用户在该场景下的桶 key
-- ARGV[1] = 补充速率（个/秒）
-- ARGV[2] = 桶容量（允许的突发量）
-- ARGV[3] = 本次消耗令牌数（正常为 1）
-- 返回 1 = 放行，0 = 拒绝

local key = KEYS[1]
local rate = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local requested = tonumber(ARGV[3])

local now = redis.call('time')
local nowMs = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)

local data = redis.call('hmget', key, 'tokens', 'ts')
local tokens = tonumber(data[1])
local ts = tonumber(data[2])

if tokens == nil then
    -- 首次访问：装满一桶，允许一次突发
    tokens = capacity
    ts = nowMs
end

local deltaMs = nowMs - ts
if deltaMs < 0 then
    -- 时钟回退保护：Redis 主从切换后可能读到更早的时间，按 0 处理而不是反向扣令牌
    deltaMs = 0
end

tokens = math.min(capacity, tokens + deltaMs / 1000 * rate)

local allowed = 0
if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
end

redis.call('hset', key, 'tokens', tokens, 'ts', nowMs)
-- 桶从空到满所需时间即 key 的最大存活期：过期自动回收，不用额外的清理任务
redis.call('pexpire', key, math.ceil(capacity / rate * 1000) + 1000)

return allowed
