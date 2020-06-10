package com.yerbie.health;

import com.codahale.metrics.health.HealthCheck;
import redis.clients.jedis.Jedis;

public class YerbieHealthCheck extends HealthCheck {

    private final Jedis jedis;

    public YerbieHealthCheck(Jedis jedis) {
        this.jedis = jedis;
    }

    @Override
    protected Result check() throws Exception {
        jedis.ping();
        return Result.healthy();
    }
}
