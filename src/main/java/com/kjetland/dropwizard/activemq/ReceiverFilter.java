package com.kjetland.dropwizard.activemq;

import javax.jms.Message;

/**
 * A filter used upon receiving a JMS message
 */
public interface ReceiverFilter {

    void apply(Message message);
}
