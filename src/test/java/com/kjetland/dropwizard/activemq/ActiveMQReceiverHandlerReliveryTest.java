package com.kjetland.dropwizard.activemq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActiveMQReceiverHandlerReliveryTest {

    final String url = "tcp://localhost:31219?" +
            "jms.redeliveryPolicy.maximumRedeliveries=3" +
            "&jms.redeliveryPolicy.initialRedeliveryDelay=100" +
            "&jms.redeliveryPolicy.redeliveryDelay=100";

    BrokerService broker;

    @BeforeEach
    public void setUp() throws Exception {
        broker = new BrokerService();
        // configure the broker
        broker.addConnector(url);
        broker.start();

        errorCount = 0;
        okCount = 0;
    }

    @AfterEach
    public void tearDown() throws Exception {
        broker.stop();
        // Just give the broker some time to stop
        Thread.sleep(1500);
    }

    int errorCount;
    int okCount;

    private void receiveMessage(String message) {

        if (message.equals("fail")) {
            errorCount++;
            throw new RuntimeException("Error in receiveMessage");
        } else {
            okCount++;
            System.out.println("receiveMessage: " + message);
        }
    }

    public boolean exceptionHandler(String message, Exception exception) {
        System.out.println("exceptionHandler: " + message + " - " + exception.getMessage());
        return false;
    }

    @Test
    public void testRedeliveryQueue() throws Exception {
        doTestRedelivery("queue:someQueue");
    }

    @Test
    public void testRedeliveryTopic() throws Exception {
        doTestRedelivery("topic:someTopic");
    }

    private void doTestRedelivery(String destinationName) throws Exception {
        ActiveMQConnectionFactory realConnectionFactory = new ActiveMQConnectionFactory(url);
        PooledConnectionFactory connectionFactory = new PooledConnectionFactory();
        connectionFactory.setConnectionFactory(realConnectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();

        ActiveMQReceiverHandler<String> h = new ActiveMQReceiverHandler<>(
                destinationName,
                connectionFactory,
                (m) -> receiveMessage(m),
                String.class,
                objectMapper,
                (m, e) -> exceptionHandler(m, e),
                1);

        h.start();

        ActiveMQSender sender = new ActiveMQSenderImpl(connectionFactory, objectMapper, destinationName, Optional.<Integer>empty(), false);

        sender.sendJson("fail");
        sender.sendJson("ok1");
        sender.sendJson("ok2");

        Thread.sleep(1000);

        assertEquals(3 + 1, errorCount);
        assertEquals(2, okCount);
    }
}
