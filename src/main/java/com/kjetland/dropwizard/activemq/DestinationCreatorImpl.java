package com.kjetland.dropwizard.activemq;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

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
