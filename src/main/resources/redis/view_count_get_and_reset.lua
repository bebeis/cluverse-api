local value = redis.call('GET', KEYS[1])

if not value then
    return 0
end

-- KEEPTTL: 일반 SET 은 TTL 을 벗겨내 0 값 고아 키를 영구히 남긴다
redis.call('SET', KEYS[1], 0, 'KEEPTTL')
return tonumber(value)
