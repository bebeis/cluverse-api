-- 증가와 TTL 무장을 원자로 묶는다 — 정리 후 라우팅 캐시 지연(~3s) 구간에
-- 재생성된 고아 키가 영구히 남는 것을 막는다. 활성 키는 매 증가마다 TTL이
-- 밀리므로 만료되지 않고, 유입이 끊긴 고아 키만 TTL 뒤에 사라진다.
local value = redis.call('INCRBY', KEYS[1], ARGV[2])
redis.call('PEXPIRE', KEYS[1], ARGV[1])
return value
