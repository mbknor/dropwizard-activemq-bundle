package example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kjetland.dropwizard.activemq.ActiveMQConfig;
import com.kjetland.dropwizard.activemq.ActiveMQConfigHolder;
import io.dropwizard.Configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class Config extends Configuration implements ActiveMQConfigHolder {

    @JsonProperty
    @NotNull
    @Valid
    private ActiveMQConfig activeMQ;

    @JsonProperty
    @NotNull
    private String queueName;

    public ActiveMQConfig getActiveMQ() {
        return activeMQ;
    }

    public String getQueueName() {
        return queueName;
    }
}


