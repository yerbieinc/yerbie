package com.yerbie.core;

import com.fasterxml.jackson.annotation.JsonProperty;

// TODO(claudio.wilson): Add more configuration.
public class RedisConfiguration {

  @JsonProperty("host")
  String host;

  @JsonProperty("port")
  Integer port;

  public String getHost() {
    return host;
  }

  public Integer getPort() {
    return port;
  }
}
