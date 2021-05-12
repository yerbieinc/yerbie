package com.yerbie;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlets.MetricsServlet;
import com.yerbie.core.*;
import com.yerbie.core.job.JobSerializer;
import com.yerbie.health.YerbieHealthCheck;
import com.yerbie.resources.JobResource;
import io.dropwizard.Application;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.Properties;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class YerbieApplication extends Application<YerbieConfiguration> {

  private static final Logger LOGGER = LoggerFactory.getLogger(YerbieApplication.class);
  private static final MetricRegistry metricRegistry = new MetricRegistry();

  private static final String ACQUIRE_LOCK_LUA_SCRIPT_FILENAME = "acquire_lock.lua";
  private static final String HAS_LOCK_LUA_SCRIPT_FILENAME = "has_lock.lua";
  private static final String APPLICATION_PROPERTIES_FILENAME = "application.properties";

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
    environment.jersey().register(new JobResource(jobManager, metricRegistry));

    environment
        .admin()
        .addServlet("metricsServlet", new MetricsServlet(metricRegistry))
        .addMapping("/yerbieMetrics");

    environment
        .lifecycle()
        .manage(
            new JobSchedulerHandler(
                jobManager,
                Clock.systemUTC(),
                locking,
                Executors.newSingleThreadScheduledExecutor()));
    LOGGER.info("Yerbie version={} started.", getYerbieVersion());
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

  private String getYerbieVersion() {
    Properties props = new Properties();

    try (InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream(APPLICATION_PROPERTIES_FILENAME)) {
      props.load(inputStream);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return props.getProperty("yerbie.version");
  }
}
