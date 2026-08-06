local current_version = redis.call('GET', KEYS[3]) or '0'
if current_version ~= ARGV[1] then
    return 0
end

redis.call('DEL', KEYS[1], KEYS[2])
for index = 3, #ARGV, 2 do
    redis.call('ZADD', KEYS[1], ARGV[index + 1], ARGV[index])
end

local ttl = tonumber(ARGV[2])
if redis.call('EXISTS', KEYS[1]) == 1 then
    redis.call('EXPIRE', KEYS[1], ttl)
end
redis.call('SET', KEYS[2], '1', 'EX', ttl)
return 1
