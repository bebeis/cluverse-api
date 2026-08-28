-- ready가 없으면 워밍 전/무효화 후 상태다. ID가 0개인 정상 캐시와 구분하기 위해 -1을 반환한다.
if redis.call('EXISTS', KEYS[2]) == 0 then
    return {-1}
end

-- 개수와 ID slice를 같은 시점에 읽어 페이지 데이터와 상한 카운트 근거가 어긋나지 않게 한다.
local result = {redis.call('ZCARD', KEYS[1])}
local members = redis.call('ZREVRANGE', KEYS[1], ARGV[1], ARGV[2])
for _, member in ipairs(members) do
    table.insert(result, member)
end
return result
