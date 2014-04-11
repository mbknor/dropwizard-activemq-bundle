package com.kjetland.dropwizard.activemq;

public interface ActiveMQSenderFactory {
    ActiveMQSender createSender(String destination, boolean persistent);
}
