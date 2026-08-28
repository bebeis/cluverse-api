-- 임대 만료 뒤 다른 요청이 얻은 락을 이전 소유자가 삭제하지 못하게 한다.
if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
end
return 0
