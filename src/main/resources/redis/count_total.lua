local current = redis.call('HGET', KEYS[1], 'count')
if not current then
    return {-1, 0}
end
local acquired = redis.call('SET', KEYS[2], '1', 'NX', 'EX', ARGV[1])
if not acquired then
    return {0, tonumber(current)}
end
current = redis.call('HINCRBY', KEYS[1], 'count', 1)
redis.call('HSET', KEYS[1], 'last_counted_at', ARGV[2])
return {1, tonumber(current)}
