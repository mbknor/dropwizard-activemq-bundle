package com.kjetland.dropwizard.activemq;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActiveMQSenderImplTest {

    @Mock
    private ConnectionFactory connectionFactory;
    @Mock
    private Connection connection;
    @Mock
    private Session session;
    @Mock
    private Queue queue;
    @Mock
    private MessageProducer messageProducer;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private TextMessage textMessage;

    @BeforeEach
    public void setUp() throws Exception {
        when(connectionFactory.createConnection()).thenReturn(connection);
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
    }

    @Test
    public void testSendSimpleQueueWithCreatorFunction() throws Exception {
        final String queueName = "myQueue";
        final String myJson = "{'a': 2, 'b': 'Some text'}";
        final String myCorrelationId = UUID.randomUUID().toString();
        final ActiveMQSender sender = new ActiveMQSenderImpl(connectionFactory, objectMapper, queueName, Optional.<Integer>empty(), false);

        when(session.createQueue(queueName)).thenReturn(queue);
        when(session.createProducer(queue)).thenReturn(messageProducer);
        when(session.createTextMessage()).thenReturn(textMessage);

        // Send a message
        sender.send((Session session) -> {
            TextMessage message = session.createTextMessage();
            message.setText(myJson);
            message.setJMSCorrelationID(myCorrelationId);
            message.setJMSReplyTo(queue);
            return message;
        });
/*
        sender.send((Session session) -> {
            try {
                TextMessage message = session.createTextMessage();
                message.setText(myJson);
                message.setJMSCorrelationID(myCorrelationId);
                message.setJMSReplyTo(queue);
                return message;
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        });
*/

        // Verify that the message was constructed as intended
        verify(textMessage).setText(myJson);
        verify(textMessage).setJMSCorrelationID(myCorrelationId);
        verify(textMessage).setJMSReplyTo(queue);
        // Verify that the message was sent by the producer
        verify(messageProducer).send(textMessage);
        verify(messageProducer).setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        // Verify that everything was cleaned up afterwards
        verify(messageProducer).close();
        verify(session).close();
        verify(connection).close();
        verifyNoMoreInteractions(textMessage, messageProducer);
    }


    @Test
    public void testSendSimpleQueueWithCreatorFunctionWhenExceptionIsThrown() throws Exception {
        final String queueName = "myQueue";
        final String myJson = "{'a': 2, 'b': 'Some text'}";
        final String myCorrelationId = UUID.randomUUID().toString();
        final ActiveMQSender sender = new ActiveMQSenderImpl(connectionFactory, objectMapper, queueName, Optional.<Integer>empty(), false);
        final JMSException thrownException = new JMSException("Test");

        when(session.createQueue(queueName)).thenReturn(queue);
        when(session.createProducer(queue)).thenReturn(messageProducer);
        doThrow(thrownException).when(session).createTextMessage();

        // Send a message and verify that a wrapped RuntimeException is thrown
        try {

            sender.send((Session session) -> {
                TextMessage message = session.createTextMessage();
                message.setText(myJson);
                message.setJMSCorrelationID(myCorrelationId);
                return message;
            });
            /*
            sender.send((Session session) -> {
                try {
                    TextMessage message = session.createTextMessage();
                    message.setText(myJson);
                    message.setJMSCorrelationID(myCorrelationId);
                    message.setJMSReplyTo(queue);
                    return message;
                } catch (JMSException e) {
                    throw new RuntimeException(e);
                }
            });
            */
            // We should not arrive here
            fail("Expected JMSException was not thrown");
        } catch (RuntimeException re) {
            assertEquals(thrownException, re.getCause());
        }
        // Verify that the message was not sent by the producer
        verify(messageProducer, never()).send(any(Message.class));
        // Verify that everything was cleaned up afterwards
        verify(messageProducer).close();
        verify(session).close();
        verify(connection).close();
    }
}