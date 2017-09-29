package com.kjetland.dropwizard.activemq;

import javax.jms.Message;

/**
 * A filter used upon sending a JMS message
 */
public interface SenderFilter {

    void apply(Message message);
}
