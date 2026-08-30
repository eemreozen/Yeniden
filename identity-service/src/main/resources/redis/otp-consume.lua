-- A stale UUID cannot consume attempts from the replacement challenge.
if redis.call('HGET', KEYS[1], 'id') ~= ARGV[1]
    or redis.call('HGET', KEYS[1], 'status') ~= 'active'
    or redis.call('PTTL', KEYS[1]) <= 0 then
    return 0
end
local attempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
if attempts > tonumber(ARGV[3]) then
    redis.call('DEL', KEYS[1])
    return 0
end
-- Compare fixed-length HMAC digests without early return on their contents.
local expected = redis.call('HGET', KEYS[1], 'hash') or ''
local difference = bit.bxor(string.len(expected), string.len(ARGV[2]))
for i = 1, string.len(expected) do
    difference = bit.bor(difference, bit.bxor(string.byte(expected, i), string.byte(ARGV[2], i) or 0))
end
if difference == 0 then
    redis.call('DEL', KEYS[1])
    return 1
end
if attempts >= tonumber(ARGV[3]) then redis.call('DEL', KEYS[1]) end
return 0
