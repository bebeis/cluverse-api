-- 버전을 먼저 올려 이미 DB를 읽고 있는 워밍 요청의 replaceIfVersion을 실패시킨다.
redis.call('INCR', KEYS[3])
redis.call('EXPIRE', KEYS[3], 86400)
-- ready까지 지워 다음 읽기가 캐시 miss로 판단되게 한다.
redis.call('DEL', KEYS[1], KEYS[2])
return 1
