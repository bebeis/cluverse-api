if redis.call('EXISTS', KEYS[2]) == 0 then
    return {-1}
end

local result = {redis.call('ZCARD', KEYS[1])}
local members = redis.call('ZREVRANGE', KEYS[1], ARGV[1], ARGV[2])
for _, member in ipairs(members) do
    table.insert(result, member)
end
return result
