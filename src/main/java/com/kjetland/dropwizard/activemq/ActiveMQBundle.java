package com.kjetland.dropwizard.activemq;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Session;

public class ActiveMQBundle implements ConfiguredBundle<ActiveMQConfigHolder>, Managed, ActiveMQSenderFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ActiveMQConnectionFactory realConnectionFactory;
    private PooledConnectionFactory connectionFactory = null;
    private ObjectMapper objectMapper;
    private Environment environment;
    private long shutdownWaitInSeconds;


    public ActiveMQBundle() {

    }

    @Override
    public void run(ActiveMQConfigHolder configuration, Environment environment) throws Exception {
        this.environment = environment;
        final String brokerUrl = configuration.getActiveMQ().brokerUrl;

        log.info("Setting up activeMq with brokerUrl {}", brokerUrl);

        realConnectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        connectionFactory = new PooledConnectionFactory();
        connectionFactory.setConnectionFactory(realConnectionFactory);

        objectMapper = environment.getObjectMapper();

        environment.lifecycle().manage(this);
        environment.healthChecks().register("ActiveMQ",
                new ActiveMQHealthCheck(connectionFactory, configuration.getActiveMQ().healthCheckMillisecondsToWait));
        this.shutdownWaitInSeconds = configuration.getActiveMQ().shutdownWaitInSeconds;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {

    }

    @Override
    public void start() throws Exception {
        log.info("Starting activeMQ client");
        connectionFactory.start();
    }

    @Override
    public void stop() throws Exception {
        log.info("Stopping activeMQ client");
        connectionFactory.stop();
    }

    public ActiveMQSender createSender(String destination, boolean persistent) {
        final Session session;
        try {
            // todo: close connection
            session = connectionFactory.createConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
        return new ActiveMQSenderImpl(session, objectMapper, destination, persistent );
    }

    // This must be used during run-phase
    public <T> void registerReceiver(String destination, ActiveMQReceiver<T> receiver, Class<? extends T> clazz, final boolean ackMessageOnException ) {

        ActiveMQReceiverHandler<T> handler = null;
        try {
            handler = new ActiveMQReceiverHandler<T>(
                    destination,
                    connectionFactory.createConnection(),
                    receiver,
                    clazz,
                    objectMapper,
                    (message, exception) -> {
                        if (ackMessageOnException) {
                            log.error("Error processing received message - acknowledging it anyway", exception);
                            return true;
                        } else {
                            log.error("Error processing received message - NOT acknowledging it", exception);
                            return false;
                        }
                    },
                    shutdownWaitInSeconds);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        environment.lifecycle().manage(handler);
    }

    // This must be used during run-phase
    public <T> void registerReceiver(String destination, ActiveMQReceiver<T> receiver, Class<? extends T> clazz, ActiveMQExceptionHandler exceptionHandler ) {

        ActiveMQReceiverHandler<T> handler = null;
        try {
            handler = new ActiveMQReceiverHandler<T>(
                    destination,
                    connectionFactory.createConnection(),
                    receiver,
                    clazz,
                    objectMapper,
                    exceptionHandler,
                    shutdownWaitInSeconds);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        environment.lifecycle().manage(handler);
    }

}
