package com.kjetland.dropwizard.activemq;

import jakarta.jms.Message;
import jakarta.jms.Session;

public interface ActiveMQSender {

    void sendJson(String json);
    void send(Object object);
    void send(JMSFunction<Session, Message> messageCreator);
}
