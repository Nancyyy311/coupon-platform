-- ============================================================
-- 验证码原子校验（消费一次即失效）
--
-- 为什么必须用 Lua：
--   Java 侧 "GET 出来比对 -> DEL 删除" 是两次网络往返，并发时多个请求
--   都能读到同一个码、都判断相等、都通过校验，验证码就从"一次性"变成了"多次有效"。
--   实测 6 并发同码登录，2 个请求同时通过了校验，最后撞在手机号唯一索引上
--   返回"服务器异常"。Redis 单线程执行脚本，把"比对 + 删除 + 计数"合成一个原子动作。
--
-- KEYS[1] = login:code:{phone}       验证码
-- KEYS[2] = login:code:fail:{phone}  失败计数
-- ARGV[1] = 用户提交的验证码
-- ARGV[2] = 失败计数窗口（秒），与验证码 TTL 对齐
-- ARGV[3] = 最大失败次数，达到即烧掉验证码
--
-- 返回：1 校验通过（码已删除）  0 不通过  2 失败次数超限（码已烧掉）
--
-- 注：两个 key 没有加相同 hash tag。单机 Redis 不受影响；
--     将来若上 Cluster，这两个 key 会落在不同 slot 而报 CROSSSLOT，届时需改成
--     login:code:{phone} 与 login:code:fail:{phone} 这种带 tag 的写法。
-- ============================================================
local code = redis.call('GET', KEYS[1])
if code == false then
    -- 没发过 / 已用过 / 已过期。不区分三种情况：都不该让调用方知道区别
    return 0
end

if code == ARGV[1] then
    redis.call('DEL', KEYS[1])
    redis.call('DEL', KEYS[2])
    return 1
end

-- 码不对：累计失败次数。
-- 只计数不删除，是为了允许用户手误输错一次再重试；
-- 但必须设上限，否则 6 位数字在 2 分钟窗口内可以被穷举完（100 万种，纯 Redis GET 很快）。
local fails = redis.call('INCR', KEYS[2])
if fails == 1 then
    redis.call('EXPIRE', KEYS[2], ARGV[2])
end
if fails >= tonumber(ARGV[3]) then
    redis.call('DEL', KEYS[1])
    redis.call('DEL', KEYS[2])
    return 2
end

return 0
