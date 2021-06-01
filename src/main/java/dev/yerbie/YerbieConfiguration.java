package dev.yerbie;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

public class YerbieConfiguration extends Configuration {

  @JsonProperty("redis")
  RedisConfiguration redisConfiguration;
}
