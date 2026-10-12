if redis.call('EXISTS', KEYS[1]) == 0 then
    return -1
end

local status = redis.call('HGET', KEYS[1], 'status')
if status == 'CONFIRMED' then
    return 2
end
if status == 'CANCELLED' then
    return -2
end

redis.call('HSET', KEYS[1], 'status', 'CONFIRMED')
redis.call('PEXPIRE', KEYS[1], ARGV[1])
return 1
