package com.kjetland.dropwizard.activemq;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Session;

public class DestinationCreatorImpl implements DestinationCreator {

    @Override
    public Destination create(Session session, String name) {
        try {
            if (name.startsWith("queue:")) {
                return session.createQueue(name.substring(6));
            } else if (name.startsWith("topic:")) {
                return session.createTopic(name.substring(6));
            } else {
                return session.createQueue(name);
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
