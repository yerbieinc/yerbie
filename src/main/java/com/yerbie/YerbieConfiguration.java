package com.yerbie;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yerbie.core.EndpointConfiguration;
import io.dropwizard.Configuration;

public class YerbieConfiguration extends Configuration {

    @JsonProperty("redis")
    EndpointConfiguration redisConfiguration;
}
