package com.paxos.server.model;

public class PaxosState {
    private final long acceptedId;
    private final long promisedId;
    private final String acceptedValue;

    public PaxosState(long promisedId, long acceptedId, String acceptedValue) {
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
