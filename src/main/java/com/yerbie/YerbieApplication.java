package com.yerbie;

import com.yerbie.health.YerbieHealthCheck;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import redis.clients.jedis.Jedis;

public class YerbieApplication extends Application<YerbieConfiguration> {

  public static void main(String[] args) throws Exception {
    new YerbieApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<YerbieConfiguration> bootstrap) {}

  @Override
  public void run(YerbieConfiguration configuration, Environment environment) {
    environment
        .healthChecks()
        .register(
            "redis",
            new YerbieHealthCheck(
                new Jedis(
                    configuration.redisConfiguration.getHost(),
                    configuration.redisConfiguration.getPort())));
  }
}
