package com.kjetland.dropwizard.activemq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveMQUtils {
    private final static Logger log = LoggerFactory.getLogger(ActiveMQUtils.class);

    public interface RunnableThrowsAll {
        void run() throws Exception;
    }

    // swallows exceptions
    public static void silent( RunnableThrowsAll runnable ) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.debug("Suppressed exception", e);
        }
    }
}
