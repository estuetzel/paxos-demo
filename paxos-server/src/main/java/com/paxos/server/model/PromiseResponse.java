package com.paxos.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromiseResponse {
    
    private boolean ignored;
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

}
