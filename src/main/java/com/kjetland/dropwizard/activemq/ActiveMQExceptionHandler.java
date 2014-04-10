package com.kjetland.dropwizard.activemq;

public interface ActiveMQExceptionHandler {

    // Return true to ack the message, false do not ack it
    boolean onException(String message, Exception exception);
}
