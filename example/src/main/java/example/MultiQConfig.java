package example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kjetland.dropwizard.activemq.ActiveMQConfig;
import com.kjetland.dropwizard.activemq.ActiveMQMultiConfigHolder;
import io.dropwizard.Configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class MultiQConfig extends Configuration implements ActiveMQMultiConfigHolder {

    @JsonProperty
    @NotNull
    @Valid
    private Map<String, ActiveMQConfig> activeMQConnections;


    public Map<String, ActiveMQConfig> getActiveMQConnections() {
        return activeMQConnections;
    }
}


