package com.kjetland.dropwizard.activemq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

public class ActiveMQSenderImpl implements ActiveMQSender {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;
    private final String destination;
    private final boolean persistent;
    protected final DestinationCreator destinationCreator = new DestinationCreatorImpl();


    public ActiveMQSenderImpl(ConnectionFactory connectionFactory, ObjectMapper objectMapper, String destination, boolean persistent) {
        this.connectionFactory = connectionFactory;
        this.objectMapper = objectMapper;
        this.destination = destination;
        this.persistent = persistent;
    }

    @Override
    public void send(Object object) {
        try {

            final String json = objectMapper.writeValueAsString(object);
            internalSend(json);

        } catch (Exception e) {
            throw new RuntimeException("Error sending to jms", e);
        }

    }

    @Override
    public void sendJson(String json) {
        try {

            internalSend(json);

        } catch (Exception e) {
            throw new RuntimeException("Error sending to jms", e);
        }

    }

    private void internalSend(String json) throws JMSException {
        log.info("Sending to {}: {}", destination, json);

        // Since we're using the pooled connectionFactory,
        // we can create connection, session and producer on the fly here.
        // as long as we do the cleanup / return to pool

        final Connection connection = connectionFactory.createConnection();
        try {

            final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            try {

                final Destination d = destinationCreator.create(session, destination);
                final MessageProducer messageProducer = session.createProducer(d);
                try {
                    messageProducer.setDeliveryMode(persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);

                    final TextMessage textMessage = session.createTextMessage(json);
                    textMessage.setText(json);
                    messageProducer.send(textMessage);

                } finally {
                    ActiveMQUtils.silent( () -> messageProducer.close() );
                }
            } finally {
                ActiveMQUtils.silent( () -> session.close() );
            }

        } finally {
            ActiveMQUtils.silent( () -> connection.close() );
        }
        final Session session;


    }
}
