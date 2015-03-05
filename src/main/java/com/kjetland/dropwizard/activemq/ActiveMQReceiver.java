package com.kjetland.dropwizard.activemq;

import java.util.Properties;

public interface ActiveMQReceiver<T> extends ActiveMQBaseReceiver<T> {

    @Override
    default void receive(T message, Properties messageProperties) {
        receive(message);
    }

    void receive(T message);
}
