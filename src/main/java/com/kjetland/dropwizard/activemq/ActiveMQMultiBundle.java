package com.kjetland.dropwizard.activemq;

import com.google.common.collect.Maps;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.Map;

public class ActiveMQMultiBundle implements ConfiguredBundle<ActiveMQMultiConfigHolder> {
    private Map<String, ActiveMQBundle> activeMQBundleMap;

    public void run(ActiveMQMultiConfigHolder configuration, Environment environment) throws Exception {

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
