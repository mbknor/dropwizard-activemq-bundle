package com.kjetland.dropwizard.activemq;

import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DestinationCreatorImplTest {
    @Test
    public void testCreate() throws Exception {
        String name = "dest-name";
        Session session = mock(Session.class);
        Queue queue = mock(Queue.class);
        Topic topic = mock(Topic.class);
        when(session.createQueue(eq(name))).thenReturn(queue);
        when(session.createTopic(eq(name))).thenReturn(topic);

        DestinationCreator destinationCreator = new DestinationCreatorImpl();

        assertSame(topic, destinationCreator.create(session, "topic:" + name));
        assertSame(queue, destinationCreator.create(session, "queue:" + name));
        assertSame(queue, destinationCreator.create(session, name));
    }
}
