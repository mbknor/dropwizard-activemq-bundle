package com.kjetland.dropwizard.activemq;

import jakarta.jms.JMSException;

/**
 * A JMSFunction represents a function that can throw a JMSException.
 * Useful to avoid catching exceptions inside the lambda expressions when writing functions that operate on the javax.jms API.
 *
 * @param <T> The type of the input to the function
 * @param <R> The type of the result of the function
 */
@FunctionalInterface
public interface JMSFunction<T, R> {
    R apply(T t) throws JMSException;
}
