package com.kjetland.dropwizard.activemq;

import com.google.common.collect.Maps;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;

import java.util.Map;

public class ActiveMQMultiBundle implements ConfiguredBundle<ActiveMQMultiConfigHolder> {
    private Map<String, ActiveMQBundle> activeMQBundleMap;

    public void run(ActiveMQMultiConfigHolder configuration, Environment environment) {

        activeMQBundleMap = Maps.transformEntries(configuration.getActiveMQConnections(), (brokerName, activeMQConfig) -> {
            ActiveMQBundle activeMQBundle = new ActiveMQBundle(brokerName);
            activeMQBundle.init(activeMQConfig, environment);
            return activeMQBundle;
        });
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {

    }

    public Map<String, ActiveMQBundle> getActiveMQBundleMap() {
        return activeMQBundleMap;
    }

}
