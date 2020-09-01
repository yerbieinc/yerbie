if redis.call('GET', KEYS[1]) == ARGV[1]
then
  redis.call('EXPIRE', KEYS[1], 180)
  if redis.call('GET', KEYS[1]) == ARGV[1]
  then
    return 1
  end

  return 0
else
  return 0
end
