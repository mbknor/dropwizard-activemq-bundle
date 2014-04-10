package com.kjetland.dropwizard.activemq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

public class ActiveMQSenderImpl implements ActiveMQSender {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Session session;
    private final MessageProducer messageProducer;
    private final ObjectMapper objectMapper;
    private final String destination;


    public ActiveMQSenderImpl(Session session, ObjectMapper objectMapper, String destination, boolean persistent) {
        this.session = session;
        this.objectMapper = objectMapper;
        this.destination = destination;
        try {
            final Queue queue = session.createQueue(destination);
            messageProducer = session.createProducer(queue);
            messageProducer.setDeliveryMode(persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
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
        final TextMessage textMessage = session.createTextMessage(json);
        textMessage.setText(json);
        messageProducer.send(textMessage);
    }
}
