redis.call('INCR', KEYS[3])
redis.call('EXPIRE', KEYS[3], 86400)
redis.call('DEL', KEYS[1], KEYS[2])
return 1
