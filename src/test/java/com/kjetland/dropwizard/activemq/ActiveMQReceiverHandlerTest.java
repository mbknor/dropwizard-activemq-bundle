package com.kjetland.dropwizard.activemq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQMessageConsumer;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ActiveMQReceiverHandlerTest {

    public static final String THROW_EXCEPTION_IN_CONSUMER = "THROW_EXCEPTION_IN_CONSUMER";
    public static final String THROW_EXCEPTION_IN_CONSUMER_CLOSED = "THROW_EXCEPTION_IN_CONSUMER_CLOSED";
    public static final String THROW_EXCEPTION_IN_RECEIVER = "THROW_EXCEPTION_IN_RECEIVER";

    String destinationName = "ourQueue";

    @Mock
    ConnectionFactory connectionFactory;

    @Mock
    Connection connection;

    @Mock
    Session session;

    @Mock
    Queue destinationQueue;

    @Mock
    Topic destinationTopic;

    @Mock
    ActiveMQMessageConsumer messageConsumer;

    ObjectMapper objectMapper;

    int messageIndex = 0;
    List<String> messagesList;

    Set<String> receivedMessages = new ConcurrentHashSet<>();
    Set<Throwable> receivedExceptions = new ConcurrentHashSet<>();


    public void setUpMocks(List<String> messages) throws Exception {
        when(connectionFactory.createConnection()).thenReturn(connection);
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
        when(session.createQueue(anyString())).thenReturn(destinationQueue);
        when(session.createTopic(anyString())).thenReturn(destinationTopic);
        when(session.createConsumer(eq(destinationQueue))).thenReturn(messageConsumer);
        when(session.createConsumer(eq(destinationTopic))).thenReturn(messageConsumer);

        messageIndex = 0;
        messagesList = messages;
        when(messageConsumer.receive(anyLong())).then( (i) -> popMessage());
        receivedMessages.clear();
        receivedExceptions.clear();
    }

    private void receiveMessage(String m) {
        if (THROW_EXCEPTION_IN_RECEIVER.equals(m)) {
            throw new RuntimeException(THROW_EXCEPTION_IN_RECEIVER);
        }
        receivedMessages.add(m);
    }

    private TextMessage popMessage() throws Exception {

        String m = messagesList.get(messageIndex);
        messageIndex++;
        if ( messageIndex>= messagesList.size()) {
            messageIndex = 0;
        }

        if ( m == null) {
            return null;
        }

        if ( THROW_EXCEPTION_IN_CONSUMER.equals(m)) {
            throw new RuntimeException(THROW_EXCEPTION_IN_CONSUMER);
        }

        if ( THROW_EXCEPTION_IN_CONSUMER_CLOSED.equals(m)) {
            throw new javax.jms.IllegalStateException("The Consumer is closed");
        }


        TextMessage msg = mock(TextMessage.class);
        Enumeration enumeration = mock(Enumeration.class);
        when(msg.getPropertyNames()).thenReturn(enumeration);
        when(msg.getText()).thenReturn(m);
        return msg;
    }

    public boolean exceptionHandler(String message, Exception exception) {
        System.out.println("exceptionHandler: " + message + " - " + exception.getMessage());
        receivedExceptions.add(exception);
        return true;
    }

    @Test
    public void testNormal() throws Exception {
        setUpMocks(Arrays.asList(null, "a", "b", null, "d"));
        ActiveMQReceiverHandler<String> h = new ActiveMQReceiverHandler<String>(
                destinationName,
                connectionFactory,
                (m,p)->receiveMessage((String)m),
                String.class,
                objectMapper,
                (m,e) -> exceptionHandler(m,e),
                1);

        h.start();
        Thread.sleep(100);
        verify(connection, VerificationModeFactory.times(1)).start();
        Thread.sleep(200);
        assertTrue(receivedMessages.contains("a"));
        assertTrue(receivedMessages.contains("b"));
        assertTrue(receivedMessages.contains("d"));
        assertEquals(3, receivedMessages.size());
        assertTrue(receivedExceptions.size()==0);
        h.stop();

    }

    @Test
    public void testExceptionInReceiver() throws Exception {
        setUpMocks(Arrays.asList(null, "a", THROW_EXCEPTION_IN_RECEIVER, "b", null, "d"));
        ActiveMQReceiverHandler<String> h = new ActiveMQReceiverHandler<String>(
                destinationName,
                connectionFactory,
                (m,p)->receiveMessage((String)m),
                String.class,
                objectMapper,
                (m,e) -> exceptionHandler(m,e),
                1);

        h.start();
        Thread.sleep(100);
        verify(connection, VerificationModeFactory.times(1)).start();
        Thread.sleep(200);
        assertTrue(receivedMessages.contains("a"));
        assertTrue(receivedMessages.contains("b"));
        assertTrue(receivedMessages.contains("d"));
        assertEquals(3, receivedMessages.size());
        assertTrue(receivedExceptions.size()>0);
        h.stop();

    }

    @Test
    public void testExceptionInMessageConsumer() throws Exception {

        setUpMocks(Arrays.asList(null, "a", THROW_EXCEPTION_IN_CONSUMER, "b", null, "d"));
        ActiveMQReceiverHandler<String> h = new ActiveMQReceiverHandler<String>(
                destinationName,
                connectionFactory,
                (m,p)->receiveMessage(m),
                String.class,
                objectMapper,
                (m,e) -> exceptionHandler(m,e),
                1);

        h.start();
        Thread.sleep(100);
        verify(connection, VerificationModeFactory.atLeast(2)).start();
        Thread.sleep(200);
        assertTrue(receivedMessages.contains("a"));
        assertTrue(receivedMessages.contains("b"));
        assertTrue(receivedMessages.contains("d"));
        assertEquals(3, receivedMessages.size());
        assertTrue(receivedExceptions.size()==0);

        h.stop();
    }

    @Test
    public void testExceptionInMessageConsumer_ConsumerIsClosed() throws Exception {

        setUpMocks(Arrays.asList(null, "a", THROW_EXCEPTION_IN_CONSUMER_CLOSED, "b", null, "d",
                THROW_EXCEPTION_IN_CONSUMER_CLOSED, THROW_EXCEPTION_IN_CONSUMER_CLOSED));
        ActiveMQReceiverHandler<String> h = new ActiveMQReceiverHandler<String>(
                destinationName,
                connectionFactory,
                (m,p)->receiveMessage(m),
                String.class,
                objectMapper,
                (m,e) -> exceptionHandler(m, e),
                1);

        h.start();
        Thread.sleep(100);
        verify(connection, VerificationModeFactory.atLeast(2)).start();
        Thread.sleep(200);
        assertTrue(receivedMessages.contains("a"));
        assertTrue(receivedMessages.contains("b"));
        assertTrue(receivedMessages.contains("d"));
        assertEquals(3, receivedMessages.size());
        assertTrue(receivedExceptions.size() == 0);
        h.stop();
    }
}