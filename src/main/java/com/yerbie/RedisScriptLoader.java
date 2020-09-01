package com.yerbie;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisScriptLoader {
  private final JedisPool jedisPool;

  public RedisScriptLoader(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }

  public String loadScript(String filename) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.scriptLoad(getScript(filename));
    }
  }

  private String getScript(String filename) {
    InputStream luaInputstream = this.getClass().getClassLoader().getResourceAsStream(filename);
    return new BufferedReader(new InputStreamReader(luaInputstream))
        .lines()
        .collect(Collectors.joining("\n"));
  }
}
