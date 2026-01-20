package com.paxos.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromiseResponse {
    
    private boolean ignored;
    private Long id;
    
    public PromiseResponse() {}
    
    public PromiseResponse(boolean ignored, Long id) {
        this.ignored = ignored;
        this.id = id;
    }
    
    public boolean isIgnored() {
        return ignored;
    }
    
    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

}
