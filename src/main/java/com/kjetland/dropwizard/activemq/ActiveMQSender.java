package com.kjetland.dropwizard.activemq;

public interface ActiveMQSender {

    void sendJson(String json);
    void send(Object object);
}
