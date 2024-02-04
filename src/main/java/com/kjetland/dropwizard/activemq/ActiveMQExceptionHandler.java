package com.kjetland.dropwizard.activemq;

import jakarta.jms.Message;

public interface ActiveMQExceptionHandler extends ActiveMQBaseExceptionHandler {

    default boolean onException(Message jmsMessage, String message, Exception exception) {
        return onException(message, exception);
    }

    // Return true to ack the message, false do not ack it
    boolean onException(String message, Exception exception);
}
