package com.yerbie;

import com.yerbie.core.*;
import com.yerbie.core.job.JobSerializer;
import com.yerbie.health.YerbieHealthCheck;
import com.yerbie.resources.JobResource;
import io.dropwizard.Application;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.concurrent.Executors;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class YerbieApplication extends Application<YerbieConfiguration> {

  private static final String ACQUIRE_LOCK_LUA_SCRIPT_FILENAME = "acquire_lock.lua";
  private static final String HAS_LOCK_LUA_SCRIPT_FILENAME = "has_lock.lua";

  public static void main(String[] args) throws Exception {
    new YerbieApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<YerbieConfiguration> bootstrap) {}

  @Override
  public void run(YerbieConfiguration configuration, Environment environment)
      throws UnknownHostException {
    JedisPool jedisPool = buildJedisPool(configuration.redisConfiguration);
    JobSerializer jobSerializer = new JobSerializer(Jackson.newObjectMapper());
    JobManager jobManager = new JobManager(jedisPool, jobSerializer, Clock.systemUTC());
    Locking locking = buildLock(jedisPool);

    environment.healthChecks().register("redis", new YerbieHealthCheck(jedisPool));
    environment.jersey().register(new JobResource(jobManager));

    environment
        .lifecycle()
        .manage(
            new JobSchedulerHandler(
                jobManager,
                Clock.systemUTC(),
                locking,
                Executors.newSingleThreadScheduledExecutor()));
  }

  private JedisPool buildJedisPool(RedisConfiguration endpointConfiguration) {
    JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
    return new JedisPool(
        jedisPoolConfig, endpointConfiguration.getHost(), endpointConfiguration.getPort());
  }

  private Locking buildLock(JedisPool jedisPool) throws UnknownHostException {
    RedisScriptLoader redisScriptLoader = new RedisScriptLoader(jedisPool);
    String acquireScriptSha = redisScriptLoader.loadScript(ACQUIRE_LOCK_LUA_SCRIPT_FILENAME);
    String hasScriptSha = redisScriptLoader.loadScript(HAS_LOCK_LUA_SCRIPT_FILENAME);
    String lockKeyValue =
        String.format(
            "%s-%d", InetAddress.getLocalHost().getHostName(), ProcessHandle.current().pid());
    return new Locking(jedisPool, lockKeyValue, acquireScriptSha, hasScriptSha);
  }
}
