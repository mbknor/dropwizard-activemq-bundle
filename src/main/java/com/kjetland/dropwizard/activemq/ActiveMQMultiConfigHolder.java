package com.kjetland.dropwizard.activemq;

import java.util.Map;

public interface ActiveMQMultiConfigHolder {
    Map<String, ActiveMQConfig> getActiveMQConnections();
}
