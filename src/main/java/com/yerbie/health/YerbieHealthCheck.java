package com.yerbie.health;

import com.codahale.metrics.health.HealthCheck;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class YerbieHealthCheck extends HealthCheck {

  private final JedisPool jedisPool;

  public YerbieHealthCheck(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }

  @Override
  protected Result check() throws Exception {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.ping();
      return Result.healthy();
    }
  }
}
