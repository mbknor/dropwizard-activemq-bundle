package com.kjetland.dropwizard.activemq;

import com.codahale.metrics.health.HealthCheck;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActiveMQHealthCheckTest {

    final String url = "tcp://localhost:31219";
    BrokerService broker;

    @BeforeEach
    public void setUp() throws Exception {
        broker = new BrokerService();
        // configure the broker
        broker.addConnector(url);
        broker.start();
    }

    @AfterEach
    public void tearDown() throws Exception {
        broker.stop();
    }

    @Test
    public void testCheck() throws Exception {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
        ActiveMQHealthCheck h = new ActiveMQHealthCheck(connectionFactory, 3000);
        assertEquals(HealthCheck.Result.healthy(), h.check());
        assertEquals(HealthCheck.Result.healthy(), h.check());
        assertEquals(HealthCheck.Result.healthy(), h.check());
    }
}
