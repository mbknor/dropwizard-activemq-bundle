package com.kjetland.dropwizard.activemq;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.dropwizard.activemq.errors.JsonError;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActiveMQReceiverHandler<T> implements Managed, Runnable {

    private static final long SLEEP_TIME_MILLS = 10000;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String destination;
    private final ConnectionFactory connectionFactory;
    private final Class<? extends T> receiverType;
    private final ActiveMQReceiver<T> receiver;
    private final ObjectMapper objectMapper;
    private final Thread thread;
    private AtomicBoolean shouldStop = new AtomicBoolean(false);
    private AtomicBoolean isReceiving = new AtomicBoolean(false);
    private final ActiveMQExceptionHandler exceptionHandler;
    protected final DestinationCreator destinationCreator = new DestinationCreatorImpl();
    protected final long shutdownWaitInSeconds;


    public ActiveMQReceiverHandler(
            String destination,
            ConnectionFactory connectionFactory,
            ActiveMQReceiver<T> receiver,
            Class<? extends T> receiverType,
            ObjectMapper objectMapper,
            ActiveMQExceptionHandler exceptionHandler,
            long shutdownWaitInSeconds) {

        this.destination = destination;
        this.connectionFactory = connectionFactory;
        this.receiver = receiver;
        this.receiverType = receiverType;
        this.objectMapper = objectMapper;
        this.exceptionHandler = exceptionHandler;
        this.shutdownWaitInSeconds = shutdownWaitInSeconds;

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


        int errorsInARowCount = 0;
        while(!shouldStop.get()) {

            try {
                log.info("Setting up receiver for " + destination);
                final Connection connection = connectionFactory.createConnection();
                try {
                    connection.start();
                    final Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
                    try {

                        final Destination d = destinationCreator.create(session, destination);
                        final MessageConsumer messageConsumer = session.createConsumer(d);
                        try {
                            log.info("Started listening for messages on " + destination);
                            errorsInARowCount = 0;
                            isReceiving.set(true);
                            runReceiveLoop(messageConsumer);
                        } finally {
                            isReceiving.set(false);
                            ActiveMQUtils.silent(() -> messageConsumer.close());
                        }
                    } finally {
                        ActiveMQUtils.silent(() -> session.close());
                    }

                } finally {
                    ActiveMQUtils.silent(() -> connection.close());
                }
            } catch (Throwable e) {
                errorsInARowCount++;
                log.error("Uncaught exception - will try to recover", e);

                // Prevent using too much CPU when stuff does not work
                if ( errorsInARowCount > 1) {
                    log.info("Numbers of errors in a row {} - Going to sleep {} mills before retrying", errorsInARowCount, SLEEP_TIME_MILLS);
                    ActiveMQUtils.silent(() -> Thread.sleep(SLEEP_TIME_MILLS));
                }
            }
        }

        log.debug("Message-checker-thread stopped");
    }

    private void runReceiveLoop(MessageConsumer messageConsumer) throws JMSException {
        while(!shouldStop.get()) {
            if (log.isDebugEnabled()) {
                log.debug("Checking for new message");
            }
            Message message = messageConsumer.receive(200);
            if (message != null) {
                processMessage(message);
            }
        }
    }

    public HealthCheck getHealthCheck() {
        return new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                if (isReceiving.get()) {
                    return Result.healthy("Is receiving from " + destination);
                } else {
                    return Result.unhealthy("Is NOT receiving from " + destination);
                }
            }
        };
    }

}
