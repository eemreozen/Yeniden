-- Acceptance may arrive after expiration or replacement; never revive either challenge.
if redis.call('HGET', KEYS[1], 'id') ~= ARGV[1]
    or redis.call('HGET', KEYS[1], 'status') ~= 'pending' then
    return {0, 0}
end
local ttl = redis.call('PTTL', KEYS[1])
if ttl <= 0 then return {0, 0} end
redis.call('HSET', KEYS[1], 'status', 'active')
return {ttl, math.max(0, redis.call('PTTL', KEYS[2]))}
