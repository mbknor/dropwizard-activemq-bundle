package com.kjetland.dropwizard.activemq;

import com.codahale.metrics.health.HealthCheck;

import jakarta.jms.*;

public class ActiveMQHealthCheck extends HealthCheck {

    private final ConnectionFactory connectionFactory;
    private final long millisecondsToWait;

    public ActiveMQHealthCheck(ConnectionFactory connectionFactory, long millisecondsToWait) {

        this.connectionFactory = connectionFactory;
        this.millisecondsToWait = millisecondsToWait;
    }

    @Override
    protected Result check() throws Exception {

        Connection connection = connectionFactory.createConnection();
        connection.start();
        try {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            try {
                TemporaryQueue tempQueue = session.createTemporaryQueue();

                try {
                    MessageProducer producer = session.createProducer(tempQueue);
                    try {
                        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                        final String messageText = "Test message-" + System.currentTimeMillis();

                        producer.send(tempQueue, session.createTextMessage(messageText));

                        MessageConsumer consumer = session.createConsumer(tempQueue);

                        try {
                            // Wait for our testMessage
                            TextMessage receivedMessage = (TextMessage) consumer.receive(millisecondsToWait);

                            // Make sure we received the correct message
                            if (receivedMessage != null && messageText.equals(receivedMessage.getText())) {
                                return Result.healthy();
                            } else {
                                return Result.unhealthy("Did not receive testMessage via tempQueue in " +
                                        millisecondsToWait + " milliseconds");
                            }
                        } finally {
                            swallowException(consumer::close);
                        }
                    } finally {
                        swallowException(producer::close);
                    }
                } finally {
                    swallowException(tempQueue::delete);
                }
            } finally {
                swallowException(session::close);
            }
        } finally {
            swallowException(connection::close);
        }
    }

    protected interface DoCleanup {
        void doCleanup() throws Exception;
    }

    protected void swallowException(DoCleanup doCleanup) {
        try {
            doCleanup.doCleanup();
        } catch (Exception e) {
            // do nothing about it
        }
    }
}
