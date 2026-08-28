-- 반환 코드: -1=초기화 필요, 0=중복 조회, 1=새 조회 집계
local current = redis.call('HGET', KEYS[1], 'count')
if not current then
    return {-1, 0}
end
local acquired = redis.call('SET', KEYS[2], '1', 'NX', 'EX', ARGV[1])
if not acquired then
    return {0, tonumber(current)}
end
-- 중복 방지 키 선점과 카운터 증가를 같은 Lua 실행 안에서 묶는다.
current = redis.call('HINCRBY', KEYS[1], 'count', 1)
redis.call('HSET', KEYS[1], 'last_counted_at', ARGV[2])
return {1, tonumber(current)}
