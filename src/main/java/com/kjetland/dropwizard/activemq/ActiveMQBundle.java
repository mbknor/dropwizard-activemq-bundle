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

// TODO: ahh healthchecks
public class ActiveMQBundle implements ConfiguredBundle<ActiveMQConfigHolder>, Managed {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private PooledConnectionFactory connectionFactory = null;
    private ObjectMapper objectMapper;
    private Environment environment;


    public ActiveMQBundle() {

    }

    @Override
    public void run(ActiveMQConfigHolder configuration, Environment environment) throws Exception {
        this.environment = environment;
        final String brokerUrl = configuration.getActiveMQ().brokerUrl;

        log.info("Setting up activeMq with brokerUrl {}", brokerUrl);

        final ActiveMQConnectionFactory realFactory = new ActiveMQConnectionFactory(brokerUrl);
        connectionFactory = new PooledConnectionFactory();
        connectionFactory.setConnectionFactory(realFactory);

        objectMapper = environment.getObjectMapper();

        environment.lifecycle().manage(this);
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
    public <T> void registerReceiver(String destination, ActiveMQReceiver<T> receiver, Class<? extends T> clazz, boolean ackMessageOnException ) {

        ActiveMQReceiverHandler<T> handler = null;
        try {
            handler = new ActiveMQReceiverHandler<T>(
                    destination,
                    connectionFactory.createConnection(),
                    receiver,
                    clazz,
                    objectMapper,
                    ackMessageOnException);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        environment.lifecycle().manage(handler);
    }
}
