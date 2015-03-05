package com.kjetland.dropwizard.activemq;

import java.util.Properties;

public interface ActiveMQBaseReceiver<T> {

    void receive(T message, Properties messageProperties);
}
