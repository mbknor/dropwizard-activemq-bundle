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
import java.util.Optional;

public class ActiveMQBundle implements ConfiguredBundle<ActiveMQConfigHolder>, Managed, ActiveMQSenderFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ActiveMQConnectionFactory realConnectionFactory;
    private PooledConnectionFactory connectionFactory = null;
    private ObjectMapper objectMapper;
    private Environment environment;
    private long shutdownWaitInSeconds;
    private Optional<Integer> defaultTimeToLiveInSeconds;

    public ActiveMQBundle() {

    }

    @Override
    public void run(ActiveMQConfigHolder configuration, Environment environment) throws Exception {
        this.environment = environment;
        final String brokerUrl = configuration.getActiveMQ().brokerUrl;
        final int configuredTTL = configuration.getActiveMQ().timeToLiveInSeconds;
        defaultTimeToLiveInSeconds = Optional.ofNullable(configuredTTL > 0 ? configuredTTL : null);

        log.info("Setting up activeMq with brokerUrl {}", brokerUrl);

        log.debug("All activeMQ config: " + configuration.getActiveMQ());

        realConnectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        connectionFactory = new PooledConnectionFactory();
        connectionFactory.setConnectionFactory(realConnectionFactory);

        configurePool(configuration.getActiveMQ().pool);

        objectMapper = environment.getObjectMapper();

        environment.lifecycle().manage(this);

        // Must use realConnectionFactory instead of (pooled) connectionFactory for the healthCheck
        // Is needs its own connection since it is both sending and receiving.
        // If using pool, then it might block since no one is available..
        environment.healthChecks().register("ActiveMQ",
                new ActiveMQHealthCheck(
                        realConnectionFactory,
                        configuration.getActiveMQ().healthCheckMillisecondsToWait));
        this.shutdownWaitInSeconds = configuration.getActiveMQ().shutdownWaitInSeconds;
    }

    private void configurePool(ActiveMQPoolConfig poolConfig) {
        if (poolConfig == null) {
            return ;
        }

        if (poolConfig.maxConnections != null) {
            connectionFactory.setMaxConnections(poolConfig.maxConnections);
        }

        if (poolConfig.maximumActiveSessionPerConnection != null) {
            connectionFactory.setMaximumActiveSessionPerConnection(poolConfig.maximumActiveSessionPerConnection);
        }

        if (poolConfig.blockIfSessionPoolIsFull != null) {
            connectionFactory.setBlockIfSessionPoolIsFull(poolConfig.blockIfSessionPoolIsFull);
        }

        if (poolConfig.idleTimeoutSeconds != null) {
            connectionFactory.setIdleTimeout(poolConfig.idleTimeoutSeconds);
        }

        if (poolConfig.expiryTimeoutMills != null) {
            connectionFactory.setExpiryTimeout(poolConfig.expiryTimeoutMills);
        }

        if (poolConfig.createConnectionOnStartup != null) {
            connectionFactory.setCreateConnectionOnStartup(poolConfig.createConnectionOnStartup);
        }

        if (poolConfig.timeBetweenExpirationCheckMillis != null) {
            connectionFactory.setTimeBetweenExpirationCheckMillis(poolConfig.timeBetweenExpirationCheckMillis);
        }

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
        return createSender(destination, persistent, defaultTimeToLiveInSeconds);
    }

    public ActiveMQSender createSender(String destination, boolean persistent, Optional<Integer> timeToLiveInSeconds) {
        return new ActiveMQSenderImpl(connectionFactory, objectMapper, destination, timeToLiveInSeconds, persistent );
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
