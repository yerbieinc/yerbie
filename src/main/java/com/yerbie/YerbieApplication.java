package com.yerbie;

import com.yerbie.core.JobFailureHandler;
import com.yerbie.core.JobManager;
import com.yerbie.core.JobScheduler;
import com.yerbie.core.RedisConfiguration;
import com.yerbie.core.job.JobSerializer;
import com.yerbie.health.YerbieHealthCheck;
import com.yerbie.resources.JobResource;
import io.dropwizard.Application;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.time.Clock;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class YerbieApplication extends Application<YerbieConfiguration> {

  public static void main(String[] args) throws Exception {
    new YerbieApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<YerbieConfiguration> bootstrap) {}

  @Override
  public void run(YerbieConfiguration configuration, Environment environment) {
    JedisPool jedisPool = buildJedisPool(configuration.redisConfiguration);
    JobSerializer jobSerializer = new JobSerializer(Jackson.newObjectMapper());
    environment.healthChecks().register("redis", new YerbieHealthCheck(jedisPool));
    JobManager jobManager = new JobManager(jedisPool, jobSerializer, Clock.systemUTC());
    environment.jersey().register(new JobResource(jobManager));
    environment.lifecycle().manage(new JobScheduler(jobManager, Clock.systemUTC()));
    environment.lifecycle().manage(new JobFailureHandler(jobManager, Clock.systemUTC()));
  }

  private JedisPool buildJedisPool(RedisConfiguration endpointConfiguration) {
    JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
    return new JedisPool(
        jedisPoolConfig, endpointConfiguration.getHost(), endpointConfiguration.getPort());
  }
}
