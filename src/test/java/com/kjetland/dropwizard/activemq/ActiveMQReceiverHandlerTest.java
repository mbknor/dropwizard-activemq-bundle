package com.kjetland.dropwizard.activemq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQMessageConsumer;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    List<Object> messagesList;

    Set<String> receivedMessages = ConcurrentHashMap.newKeySet();
    Set<Throwable> receivedExceptions = ConcurrentHashMap.newKeySet();


    public void setUpMocks(Object... messages) throws Exception {
        when(connectionFactory.createConnection()).thenReturn(connection);
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
        when(session.createQueue(anyString())).thenReturn(destinationQueue);
        when(session.createTopic(anyString())).thenReturn(destinationTopic);
        when(session.createConsumer(eq(destinationQueue))).thenReturn(messageConsumer);
        when(session.createConsumer(eq(destinationTopic))).thenReturn(messageConsumer);

        messageIndex = 0;
        messagesList = Arrays.asList(messages);
        when(messageConsumer.receive(anyLong())).then((i) -> popMessage());
        receivedMessages.clear();
        receivedExceptions.clear();
    }

    private void receiveMessage(String m) {
        if (THROW_EXCEPTION_IN_RECEIVER.equals(m)) {
            throw new RuntimeException(THROW_EXCEPTION_IN_RECEIVER);
        }
        receivedMessages.add(m);
    }

    private Message popMessage() throws Exception {

        Object m = messagesList.get(messageIndex);
        messageIndex++;
        if (messageIndex >= messagesList.size()) {
            messageIndex = 0;
        }

        if (m == null) {
            return null;
        }

        if (THROW_EXCEPTION_IN_CONSUMER.equals(m)) {
            throw new RuntimeException(THROW_EXCEPTION_IN_CONSUMER);
        }

        if (THROW_EXCEPTION_IN_CONSUMER_CLOSED.equals(m)) {
            throw new jakarta.jms.IllegalStateException("The Consumer is closed");
        }


        if (m instanceof String) {
            TextMessage msg = mock(TextMessage.class);
            when(msg.getText()).thenReturn((String) m);
            return msg;
        } else if (m instanceof ActiveMQObjectMessage) {
            return (ActiveMQObjectMessage) m;
        } else {
            throw new IllegalArgumentException("Message type " + m.getClass() + " is not supported yet");
        }
    }

    public boolean exceptionHandler(String message, Exception exception) {
        System.out.println("exceptionHandler: " + message + " - " + exception.getMessage());
        receivedExceptions.add(exception);
        return true;
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void testNormal() throws Exception {
        setUpMocks(null, "a", "b", null, "d");
        ActiveMQReceiverHandler<String> h = new ActiveMQReceiverHandler<>(
                destinationName,
                connectionFactory,
                (m) -> receiveMessage((String) m),
                String.class,
                objectMapper,
                (m, e) -> exceptionHandler(m, e),
                1);

        h.start();
        Thread.sleep(100);
        verify(connection, VerificationModeFactory.times(1)).start();
        Thread.sleep(200);
        assertTrue(receivedMessages.contains("a"));
        assertTrue(receivedMessages.contains("b"));
        assertTrue(receivedMessages.contains("d"));
        assertEquals(3, receivedMessages.size());
        assertTrue(receivedExceptions.size() == 0);
        h.stop();

    }

    static class MessagePayload implements Serializable {
        private final String payLoad;

        MessagePayload(String payLoad) {
            this.payLoad = payLoad;
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void testThatActiveMQObjectMessageIsSupported() throws Exception {
        final ActiveMQObjectMessage OBJECT_MESSAGE = new ActiveMQObjectMessage();
        MessagePayload OBJECT = new MessagePayload("Some data");
        OBJECT_MESSAGE.setObject(OBJECT);

        Set<Throwable> RECEIVED_EXCEPTIONS = new CopyOnWriteArraySet<>();
        AtomicReference<MessagePayload> RECEIVED_OBJECT = new AtomicReference<>();

        setUpMocks(OBJECT_MESSAGE);
        ActiveMQReceiverHandler<MessagePayload> h = new ActiveMQReceiverHandler<>(
                destinationName,
                connectionFactory,
                RECEIVED_OBJECT::set,
                MessagePayload.class,
                objectMapper,
                (m, e) -> RECEIVED_EXCEPTIONS.add(e),
                1);

        h.start();
        Thread.sleep(300);
        RECEIVED_EXCEPTIONS.forEach(Throwables::propagate);
        assertNotNull(RECEIVED_OBJECT.get());
        assertEquals(OBJECT, RECEIVED_OBJECT.get());
        h.stop();

    }

    private static class ExtendedPayload extends MessagePayload {

        private String extraData = "EXTRA_DATA";

        ExtendedPayload(String payLoad) {
            super(payLoad);
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void testThatCanReceiveObjectsOfSubtype() throws Exception {
        final ActiveMQObjectMessage OBJECT_MESSAGE = new ActiveMQObjectMessage();
        ExtendedPayload OBJECT = new ExtendedPayload("Some data");
        OBJECT_MESSAGE.setObject(OBJECT);

        Set<Throwable> RECEIVED_EXCEPTIONS = new CopyOnWriteArraySet<>();
        AtomicReference<MessagePayload> RECEIVED_OBJECT = new AtomicReference<>();

        setUpMocks(OBJECT_MESSAGE);
        ActiveMQReceiverHandler<MessagePayload> h = new ActiveMQReceiverHandler<>(
                destinationName,
                connectionFactory,
                RECEIVED_OBJECT::set,
                MessagePayload.class,
                objectMapper,
                (m, e) -> RECEIVED_EXCEPTIONS.add(e),
                1);

        h.start();
        Thread.sleep(300);
        RECEIVED_EXCEPTIONS.forEach(Throwables::propagate);
        assertNotNull(RECEIVED_OBJECT.get());
        assertEquals(OBJECT, RECEIVED_OBJECT.get());
        h.stop();

    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void testThatObjectConsumptionFailsIfReceivedTypeIsWrong() throws Exception {
        final ActiveMQObjectMessage OBJECT_MESSAGE = new ActiveMQObjectMessage();
        MessagePayload OBJECT = new MessagePayload("Some data");
        OBJECT_MESSAGE.setObject(OBJECT);

        Set<Throwable> RECEIVED_EXCEPTIONS = new CopyOnWriteArraySet<>();
        AtomicReference<String> RECEIVED_OBJECT = new AtomicReference<>();

        setUpMocks(OBJECT_MESSAGE);
        ActiveMQReceiverHandler<String> h = new ActiveMQReceiverHandler<>(
                destinationName,
                connectionFactory,
                RECEIVED_OBJECT::set,
                String.class,
                objectMapper,
                (m, e) -> RECEIVED_EXCEPTIONS.add(e),
                1);

        h.start();
        Thread.sleep(300);
        assertFalse(RECEIVED_EXCEPTIONS.isEmpty());
        assertNull(RECEIVED_OBJECT.get());
        h.stop();

    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void testExceptionInReceiver() throws Exception {
        setUpMocks(null, "a", THROW_EXCEPTION_IN_RECEIVER, "b", null, "d");
        ActiveMQReceiverHandler<String> h = new ActiveMQReceiverHandler<>(
                destinationName,
                connectionFactory,
                (m) -> receiveMessage((String) m),
                String.class,
                objectMapper,
                (m, e) -> exceptionHandler(m, e),
                1);

        h.start();
        Thread.sleep(100);
        verify(connection, VerificationModeFactory.times(1)).start();
        Thread.sleep(200);
        assertTrue(receivedMessages.contains("a"));
        assertTrue(receivedMessages.contains("b"));
        assertTrue(receivedMessages.contains("d"));
        assertEquals(3, receivedMessages.size());
        assertTrue(receivedExceptions.size() > 0);
        h.stop();

    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void testExceptionInMessageConsumer() throws Exception {

        setUpMocks(null, "a", THROW_EXCEPTION_IN_CONSUMER, "b", null, "d");
        ActiveMQReceiverHandler<String> h = new ActiveMQReceiverHandler<>(
                destinationName,
                connectionFactory,
                (m) -> receiveMessage(m),
                String.class,
                objectMapper,
                (m, e) -> exceptionHandler(m, e),
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

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    public void testExceptionInMessageConsumer_ConsumerIsClosed() throws Exception {

        setUpMocks(null, "a", THROW_EXCEPTION_IN_CONSUMER_CLOSED, "b", null, "d",
                THROW_EXCEPTION_IN_CONSUMER_CLOSED, THROW_EXCEPTION_IN_CONSUMER_CLOSED);
        ActiveMQReceiverHandler<String> h = new ActiveMQReceiverHandler<>(
                destinationName,
                connectionFactory,
                (m) -> receiveMessage(m),
                String.class,
                objectMapper,
                (m, e) -> exceptionHandler(m, e),
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