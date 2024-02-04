package com.kjetland.dropwizard.activemq;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ActiveMQHealthCheckTest {
    private final String url = "tcp://localhost:31219";
    private BrokerService broker;

    @BeforeEach
    void setUp() throws Exception {
        broker = new BrokerService();
        // configure the broker
        broker.addConnector(url);
        broker.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        broker.stop();
        Thread.sleep(1500);
    }

    @Test
    void testCheck() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
        ActiveMQHealthCheck h = new ActiveMQHealthCheck(connectionFactory, 3000);
        assertAll(
                () -> assertTrue(h.check().isHealthy()),
                () -> assertTrue(h.check().isHealthy()),
                () -> assertTrue(h.check().isHealthy())
        );
    }

    @Test
    void testCheckConnectionCloseHandling() throws Exception {
        //given
        ActiveMQConnectionFactory connectionFactory = mock(ActiveMQConnectionFactory.class);
        Connection connection = mock(Connection.class);
        when(connectionFactory.createConnection()).thenReturn(connection);

        doThrow(new JMSException("JmsError", "999")).when(connection).start();

        //when
        ActiveMQHealthCheck h = new ActiveMQHealthCheck(connectionFactory, 3000);

        //then
        assertThrows(JMSException.class, h::check);
    }
}
