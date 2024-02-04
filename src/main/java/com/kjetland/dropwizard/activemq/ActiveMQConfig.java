package com.kjetland.dropwizard.activemq;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ActiveMQConfig {

    @JsonProperty
    @NotNull
    public String brokerUrl;

    @JsonProperty
    public String brokerUsername;

    @JsonProperty
    public String brokerPassword;

    @JsonProperty
    public long healthCheckMillisecondsToWait = 2000; // 2 seconds

    @JsonProperty
    public int shutdownWaitInSeconds = 20;

    @JsonProperty
    public int timeToLiveInSeconds = -1; // Default no TTL. Jackson does not support java.util.Optional yet.

    @JsonProperty
    @Valid
    public List<String> trustedPackages = new ArrayList<>();

    @JsonProperty
    @Valid
    public ActiveMQPoolConfig pool;

    @Override
    public String toString() {
        return "ActiveMQConfig{" +
                "brokerUrl='" + brokerUrl + '\'' +
                ", healthCheckMillisecondsToWait=" + healthCheckMillisecondsToWait +
                ", shutdownWaitInSeconds=" + shutdownWaitInSeconds +
                ", timeToLiveInSeconds=" + timeToLiveInSeconds +
                ", brokerUsername=" + brokerUsername +
                ", brokerPassword=" + brokerPassword +
                ", trustedPackages=" + String.join(", ", trustedPackages) +
                ", pool=" + pool +
                '}';
    }
}
