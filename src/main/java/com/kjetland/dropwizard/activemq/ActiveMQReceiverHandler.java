package com.kjetland.dropwizard.activemq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.dropwizard.activemq.errors.JsonError;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActiveMQReceiverHandler<T> implements Managed, Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String destination;
    private final Session session;
    private final MessageConsumer messageConsumer;
    private final Class<? extends T> receiverType;
    private final ActiveMQReceiver<T> receiver;
    private final ObjectMapper objectMapper;
    private final Thread thread;
    private AtomicBoolean shouldStop = new AtomicBoolean(false);
    private final ActiveMQExceptionHandler exceptionHandler;
    protected final DestinationCreator destinationCreator = new DestinationCreatorImpl();
    protected final long shutdownWaitInSeconds;


    public ActiveMQReceiverHandler(
            String destination,
            Connection connection,
            ActiveMQReceiver<T> receiver,
            Class<? extends T> receiverType,
            ObjectMapper objectMapper,
            ActiveMQExceptionHandler exceptionHandler,
            long shutdownWaitInSeconds) {
        this.destination = destination;
        this.receiver = receiver;
        this.receiverType = receiverType;
        this.objectMapper = objectMapper;
        this.exceptionHandler = exceptionHandler;
        this.shutdownWaitInSeconds = shutdownWaitInSeconds;

        try {
            connection.start();
            this.session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            final Destination d = destinationCreator.create(session, destination);
            this.messageConsumer = session.createConsumer(d);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        this.thread = new Thread(this, "Receiver "+destination);
    }

    @Override
    public void start() throws Exception {
        log.info("Starting receiver for " + destination);
        thread.start();
    }

    @Override
    public void stop() throws Exception {
        log.info("Stopping receiver for " + destination + " (Going to wait for max " + shutdownWaitInSeconds + " seconds)");

        if (thread.isAlive()) {
            shouldStop.set(true);
            final long start = System.currentTimeMillis();
            while (thread.isAlive()) {
                if (((System.currentTimeMillis() - start) / 1000) >= shutdownWaitInSeconds) {
                    log.warn("Giving up waiting for receiver-thread shutdown");
                    break;
                }
                log.debug("ReceiverThread is still alive..");
                Thread.sleep(200);
            }
        }
        log.info("Stopped receiver for " + destination);
    }

    private void processMessage(Message message) {
        String json = null;
        try {

            if (message instanceof TextMessage) {
                json = ((TextMessage) message).getText();
            } else {
                throw new Exception("Do not know how to handle messages of type " + message.getClass());
            }

            log.info("Received " + json);

            if ( receiverType.equals(String.class)) {
                // pass the string as is
                receiver.receive((T)json);
            } else {
                T object = fromJson(json);
                receiver.receive(object);
            }

            message.acknowledge();
        } catch (Exception e) {
            if (exceptionHandler.onException(json, e)) {
                try {
                    message.acknowledge();
                } catch (JMSException x) {
                    throw new RuntimeException(x);
                }
            }
        }
    }

    private T fromJson(String json) {
        try {
            return (T)objectMapper.readValue(json, receiverType);
        } catch (IOException e) {
            throw new JsonError(e);
        }
    }

    @Override
    public void run() {

        try {
            while(!shouldStop.get()) {
                if (log.isDebugEnabled()) {
                    log.debug("Checking for new message");
                }
                Message message = messageConsumer.receive(200);
                if (message != null) {
                    processMessage(message);
                }
            }
        } catch (Throwable e) {
            log.error("Uncaught error", e);
        }
        log.debug("Message-checker-thread stopped");
    }

}
