package com.paxos.server.service;

import com.paxos.server.model.AcceptResponse;
import com.paxos.server.model.PromiseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Paxos Acceptor implementation.
 * Handles the acceptor role in the Paxos consensus protocol.
 */
@Service
public class PaxosAcceptorService {

    private static final Logger log = LoggerFactory.getLogger(PaxosAcceptorService.class);

    @Value("${paxos.server.id:0}")
    private int serverId;

    // Highest proposal number promised
    private long promisedId = -1;
    
    // Highest proposal number accepted
    private long acceptedId = -1;
    
    // Value associated with the accepted proposal
    private String acceptedValue = null;
    
    // Lock for thread safety
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Phase 1b: Prepare/Promise
     * If the proposal number is higher than any we've seen, promise not to accept
     * any proposal with a lower number.
     */
    public PromiseResponse prepare(long proposalId) {
        lock.lock();
        try {
            log.info("Server {} received prepare request with id: {}", serverId, proposalId);
            PromiseResponse ret = new PromiseResponse();
            if (proposalId > promisedId) {
                promisedId = proposalId;
                log.info("Server {} promising for id: {}", serverId, proposalId);
                ret.setId(promisedId);
            } else {
                log.info("Server {} ignoring prepare with id: {} (already promised: {})", 
                        serverId, proposalId, promisedId);
                ret.setIgnored(true);
            }
            return ret;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Phase 2b: Accept
     * If the proposal number is at least as high as any we've promised,
     * accept the proposal.
     */
    public AcceptResponse acceptRequest(long proposalId, String value) {
        lock.lock();
        try {
            log.info("Server {} received accept request with id: {}, value: {}", 
                    serverId, proposalId, value);
            
            if (proposalId >= promisedId) {
                promisedId = proposalId;
                acceptedId = proposalId;
                acceptedValue = value;
                log.info("Server {} accepted id: {}, value: {}", serverId, proposalId, value);
                return AcceptResponse.accept(acceptedId, acceptedValue);
            } else {
                log.info("Server {} ignoring accept with id: {} (promised: {})", 
                        serverId, proposalId, promisedId);
                return AcceptResponse.ignore();
            }
        } finally {
            lock.unlock();
        }
    }

    public int getServerId() {
        return serverId;
    }

    public Long getPromiseId() {
        return promisedId;
    }

    public Long getAcceptId() {
        return acceptedId;
    }
}
