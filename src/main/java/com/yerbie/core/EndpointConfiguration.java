package com.yerbie.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EndpointConfiguration {

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
