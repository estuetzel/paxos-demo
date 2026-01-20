package com.paxos.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AcceptResponse {
    
    private boolean ignored;
    private Long acceptedId;
    private String acceptedValue;
    
    public AcceptResponse() {}
    
    public AcceptResponse(boolean ignored, Long acceptedId, String acceptedValue) {
        this.ignored = ignored;
        this.acceptedId = acceptedId;
        this.acceptedValue = acceptedValue;
    }
    
    public static AcceptResponse ignore() {
        return new AcceptResponse(true, null, null);
    }
    
    public static AcceptResponse accept(long id, String value) {
        return new AcceptResponse(false, id, value);
    }
    
    public boolean isIgnored() {
        return ignored;
    }
    
    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }
    
    public Long getAcceptedId() {
        return acceptedId;
    }
    
    public void setAcceptedId(Long acceptedId) {
        this.acceptedId = acceptedId;
    }
    
    public String getAcceptedValue() {
        return acceptedValue;
    }
    
    public void setAcceptedValue(String acceptedValue) {
        this.acceptedValue = acceptedValue;
    }
}
