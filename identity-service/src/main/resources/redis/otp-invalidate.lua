-- A delayed send failure must never delete a newer challenge or reset its abuse limits.
if redis.call('HGET', KEYS[1], 'id') == ARGV[1] then
    return redis.call('DEL', KEYS[1])
end
return 0
