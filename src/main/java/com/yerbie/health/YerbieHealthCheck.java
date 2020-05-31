package com.yerbie.health;

import com.codahale.metrics.health.HealthCheck;

public class YerbieHealthCheck extends HealthCheck {

    @Override
    protected Result check() throws Exception {
        return Result.healthy();
    }
}
