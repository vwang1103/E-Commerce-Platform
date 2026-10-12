if redis.call('EXISTS', KEYS[2]) == 0 then
    return -1
end

local status = redis.call('HGET', KEYS[2], 'status')
if status == 'CANCELLED' then
    return 2
end
if status == 'CONFIRMED' then
    return -2
end

local quantity = tonumber(redis.call('HGET', KEYS[2], 'quantity'))
redis.call('INCRBY', KEYS[1], quantity)
redis.call('HSET', KEYS[2], 'status', 'CANCELLED')
redis.call('PEXPIRE', KEYS[2], ARGV[1])
return 1
