package com.paxos.server.model;

public class PaxosState {
    private final long acceptedId;
    private final long promisedId;

    public PaxosState(long acceptedId, long promisedId) {
        this.acceptedId = acceptedId;
        this.promisedId = promisedId;
    }

    public long getAcceptedId() {
        return acceptedId;
    }

    public long getPromisedId() {
        return promisedId;
    }
}
