local count = redis.call('HGET', KEYS[1], 'count')
local last_counted_at = redis.call('HGET', KEYS[1], 'last_counted_at')
if count == ARGV[1] and last_counted_at == ARGV[2] then
    return redis.call('DEL', KEYS[1])
end
return 0
