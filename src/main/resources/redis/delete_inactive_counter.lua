-- 최종 체크포인트 뒤 조회가 들어왔다면 count나 시각이 달라지므로 삭제하지 않는다.
local count = redis.call('HGET', KEYS[1], 'count')
local last_counted_at = redis.call('HGET', KEYS[1], 'last_counted_at')
if count == ARGV[1] and last_counted_at == ARGV[2] then
    return redis.call('DEL', KEYS[1])
end
return 0
