package com.yerbie;

import com.yerbie.core.JobManager;
import com.yerbie.core.Scheduler;
import com.yerbie.core.job.JobSerializer;
import com.yerbie.health.YerbieHealthCheck;
import com.yerbie.resources.JobResource;
import io.dropwizard.Application;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.time.Clock;
import redis.clients.jedis.Jedis;

public class YerbieApplication extends Application<YerbieConfiguration> {

  public static void main(String[] args) throws Exception {
    new YerbieApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<YerbieConfiguration> bootstrap) {}

  @Override
  public void run(YerbieConfiguration configuration, Environment environment) {
    Jedis jedis =
        new Jedis(
            configuration.redisConfiguration.getHost(), configuration.redisConfiguration.getPort());
    JobSerializer jobSerializer = new JobSerializer(Jackson.newObjectMapper());
    environment.healthChecks().register("redis", new YerbieHealthCheck(jedis));
    JobManager jobManager = new JobManager(jedis, jobSerializer, Clock.systemUTC());
    environment.jersey().register(new JobResource(jobManager));
    environment.lifecycle().manage(new Scheduler(jobManager, Clock.systemUTC()));
  }
}
