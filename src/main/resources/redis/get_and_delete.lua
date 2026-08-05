local value = redis.call('GET', KEYS[1])
if not value then
    return 0
end
redis.call('DEL', KEYS[1])
return tonumber(value)
