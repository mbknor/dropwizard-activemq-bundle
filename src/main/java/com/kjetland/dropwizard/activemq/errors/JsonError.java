package com.kjetland.dropwizard.activemq.errors;

public class JsonError extends RuntimeException {

    public JsonError(Throwable cause) {
        super(cause);
    }
}
