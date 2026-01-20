package com.paxos.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromiseResponse {
    
    private boolean ignored;
    private Long promisedId;
    private Long acceptedId;
    private String acceptedValue;

    
    public PromiseResponse() {}
    
    public boolean isIgnored() {
        return ignored;
    }
    
    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }
    
    public Long getAcceptedId() {
        return acceptedId;
    }

    public String getAcceptedValue() {
        return acceptedValue;
    }

    public void setAcceptedValue(String acceptedValue) {
        this.acceptedValue = acceptedValue;
    }

    public void setAcceptedId(long acceptedId) {
        this.acceptedId = acceptedId;
    }

    public Long getPromisedId() {
        return promisedId;
    }

    public void setPromisedId(Long promisedId) {
        this.promisedId = promisedId;
    }

    public void setAcceptedId(Long acceptedId) {
        this.acceptedId = acceptedId;
    }

    @Override
    public String toString() {
        return "PromiseResponse{" +
                "ignored=" + ignored +
                ", acceptedId=" + acceptedId +
                ", acceptedValue='" + acceptedValue + '\'' +
                '}';
    }
}
