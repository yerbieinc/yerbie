package com.yerbie;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yerbie.core.RedisConfiguration;
import io.dropwizard.Configuration;

public class YerbieConfiguration extends Configuration {

  @JsonProperty("redis")
  RedisConfiguration redisConfiguration;
}
