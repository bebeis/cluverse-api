-- 워밍 시작 뒤 쓰기 무효화가 한 번이라도 실행됐으면 오래된 DB 스냅숏을 저장하지 않는다.
local current_version = redis.call('GET', KEYS[3]) or '0'
if current_version ~= ARGV[1] then
    return 0
end

-- 기존 ID와 ready를 먼저 지우고 새 목록과 ready를 한 Lua 안에서 함께 공개한다.
redis.call('DEL', KEYS[1], KEYS[2])
for index = 3, #ARGV, 2 do
    redis.call('ZADD', KEYS[1], ARGV[index + 1], ARGV[index])
end

local ttl = tonumber(ARGV[2])
if redis.call('EXISTS', KEYS[1]) == 1 then
    redis.call('EXPIRE', KEYS[1], ttl)
end
-- 게시글이 0개면 Sorted Set은 없지만 ready는 남겨 정상적인 빈 캐시임을 표시한다.
redis.call('SET', KEYS[2], '1', 'EX', ttl)
return 1
