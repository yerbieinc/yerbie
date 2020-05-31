package com.yerbie;

import com.yerbie.health.YerbieHealthCheck;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class YerbieApplication extends Application<YerbieConfiguration> {

    public static void main(String[] args) throws Exception {
        new YerbieApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<YerbieConfiguration> bootstrap) {

    }

    @Override
    public void run(YerbieConfiguration configuration, Environment environment) {
        environment.healthChecks().register("default", new YerbieHealthCheck());
    }
}
