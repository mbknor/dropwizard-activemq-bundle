package com.kjetland.dropwizard.activemq;

public interface ActiveMQReceiver<T> {

    public void receive(T message);
}
