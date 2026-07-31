local value = redis.call('GET', KEYS[1])

if not value then
    return 0
end

redis.call('SET', KEYS[1], 0)
return tonumber(value)
