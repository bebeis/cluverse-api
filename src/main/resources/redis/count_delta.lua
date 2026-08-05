local acquired = redis.call('SET', KEYS[1], '1', 'NX', 'EX', ARGV[1])
if not acquired then
    local current = tonumber(redis.call('GET', KEYS[2]) or '0')
    return {0, current}
end
local current = redis.call('INCR', KEYS[2])
return {1, current}
