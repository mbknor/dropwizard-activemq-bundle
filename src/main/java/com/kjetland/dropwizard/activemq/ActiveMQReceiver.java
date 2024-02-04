package com.kjetland.dropwizard.activemq;

public interface ActiveMQReceiver<T> {

    void receive(T message);
}
