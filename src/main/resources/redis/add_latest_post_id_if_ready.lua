-- 진행 중인 워밍은 생성 전 DB 스냅숏을 저장하지 못하게 한다.
redis.call('INCR', KEYS[3])
redis.call('EXPIRE', KEYS[3], 86400)

-- 아직 조회되지 않은 게시판·카테고리에는 일부 ID만 든 캐시를 만들지 않는다.
if redis.call('EXISTS', KEYS[2]) == 0 then
    return 0
end

redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])
local max_entries = tonumber(ARGV[3])
redis.call('ZREMRANGEBYRANK', KEYS[1], 0, -max_entries - 1)

local ttl = tonumber(ARGV[4])
redis.call('EXPIRE', KEYS[1], ttl)
redis.call('EXPIRE', KEYS[2], ttl)
return 1
