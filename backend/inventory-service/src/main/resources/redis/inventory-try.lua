local available = redis.call('GET', KEYS[1])
if not available then
    return -1
end

if redis.call('EXISTS', KEYS[2]) == 1 then
    local storedUser = redis.call('HGET', KEYS[2], 'userId')
    local storedQuantity = redis.call('HGET', KEYS[2], 'quantity')
    local status = redis.call('HGET', KEYS[2], 'status')
    if storedUser ~= ARGV[1] or storedQuantity ~= ARGV[2] then
        return -3
    end
    if status == 'TRY' then
        return 2
    end
    if status == 'CONFIRMED' then
        return 3
    end
    return -4
end

local quantity = tonumber(ARGV[2])
if tonumber(available) < quantity then
    return -2
end

redis.call('DECRBY', KEYS[1], quantity)
redis.call('HSET', KEYS[2],
    'userId', ARGV[1],
    'quantity', ARGV[2],
    'status', 'TRY',
    'expiresAtEpochMilli', ARGV[3])
redis.call('PEXPIRE', KEYS[2], ARGV[4])
return 1
