if redis.call('SETNX', KEYS[1], ARGV[1]) == 1
then
  redis.call('EXPIRE', KEYS[1], 180)
  return 1
else
  return 0
end