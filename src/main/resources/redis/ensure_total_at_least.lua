-- 로컬 delta를 합치기 전에 Redis 기준값이 MySQL 체크포인트보다 낮지 않게 맞춘다.
local current = redis.call('HGET', KEYS[1], 'count')
local base = tonumber(ARGV[1])
if not current then
    redis.call('HSET', KEYS[1], 'count', base, 'last_counted_at', 0)
    return base
end
current = tonumber(current)
if current < base then
    redis.call('HSET', KEYS[1], 'count', base)
    return base
end
return current
