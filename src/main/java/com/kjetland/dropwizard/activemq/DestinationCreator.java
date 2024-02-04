package com.kjetland.dropwizard.activemq;

import jakarta.jms.Destination;
import jakarta.jms.Session;

interface DestinationCreator {
    Destination create(Session session, String name);
}
