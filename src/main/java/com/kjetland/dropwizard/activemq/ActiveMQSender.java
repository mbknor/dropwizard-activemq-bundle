package com.kjetland.dropwizard.activemq;

import javax.jms.Message;
import javax.jms.Session;

public interface ActiveMQSender {

    void sendJson(String json);
    void send(Object object);
    void send(JMSFunction<Session, Message> messageCreator);
    void addFilter(SenderFilter senderFilter);
}
