package com.kjetland.dropwizard.activemq;

import javax.jms.Destination;
import javax.jms.Session;

interface DestinationCreator {
    Destination create(Session session, String name);
}
