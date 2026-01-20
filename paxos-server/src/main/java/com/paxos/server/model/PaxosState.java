package com.paxos.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PaxosState {
    private final long acceptedId;
    private final long promisedId;
    private final String acceptedValue;

    @JsonCreator
    public PaxosState(
            @JsonProperty("promisedId") long promisedId,
            @JsonProperty("acceptedId") long acceptedId,
            @JsonProperty("acceptedValue") String acceptedValue) {
        this.acceptedId = acceptedId;
        this.promisedId = promisedId;
        this.acceptedValue = acceptedValue;
    }

    public long getAcceptedId() {
        return acceptedId;
    }

    public long getPromisedId() {
        return promisedId;
    }

    public String getAcceptedValue() {
        return acceptedValue;
    }
}
