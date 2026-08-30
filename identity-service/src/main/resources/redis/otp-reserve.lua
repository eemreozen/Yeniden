-- KEYS: challenge, cooldown, rolling phone requests, rolling IP requests.
-- ARGV: challenge UUID, HMAC proof, TTL ms, cooldown ms, phone limit, IP limit.
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
local window = 3600000
local retry = math.max(0, redis.call('PTTL', KEYS[2]))
for index = 3, 4 do
    redis.call('ZREMRANGEBYSCORE', KEYS[index], '-inf', now - window)
    if redis.call('ZCARD', KEYS[index]) >= tonumber(ARGV[index + 2]) then
        local first = redis.call('ZRANGE', KEYS[index], 0, 0, 'WITHSCORES')
        retry = math.max(retry, tonumber(first[2]) + window - now)
    end
end
if retry > 0 then return retry end

for index = 3, 4 do
    redis.call('ZADD', KEYS[index], now, ARGV[1])
    redis.call('PEXPIRE', KEYS[index], window)
end
redis.call('SET', KEYS[2], ARGV[1], 'PX', ARGV[4])
redis.call('HSET', KEYS[1], 'id', ARGV[1], 'hash', ARGV[2], 'attempts', 0, 'status', 'pending')
redis.call('PEXPIRE', KEYS[1], ARGV[3])
return 0
