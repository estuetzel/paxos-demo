package com.paxos.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paxos.server.model.AcceptResponse;
import com.paxos.server.model.PaxosState;
import com.paxos.server.model.PromiseResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Paxos Acceptor implementation.
 * Handles the acceptor role in the Paxos consensus protocol.
 * Note that this class assumes that proposers are adhering to the
 * paxos protocol and not sending invalid prepare or acceptRequest commands.
 */
@Service
public class PaxosAcceptorService {

    private static final Logger log = LoggerFactory.getLogger(PaxosAcceptorService.class);

    // Highest proposal number promised
    private long promisedId = -1;
    
    // Highest proposal number accepted
    private long acceptedId = -1;
    
    // Value associated with the accepted proposal
    private String acceptedValue = null;

    /**
     * Phase 1b: Prepare/Promise
     * If the proposal number is higher than any we've seen, promise not to accept
     * any proposal with a lower number.
     */
    public synchronized PromiseResponse prepare(long proposalId) {
        // TODO write state to file after prepare op in future version
        log.info("Received prepare request with id: {}", proposalId);
        PromiseResponse ret = new PromiseResponse();
        if (proposalId > promisedId) {
            ret.setPromisedId(proposalId);
            promisedId = proposalId;
            log.info("Promising for id: {}", proposalId);
            // when promising proposalId, return previously highest acceptedId and value
            if (acceptedId >= 0) {
                ret.setAcceptedId(acceptedId);
                ret.setAcceptedValue(acceptedValue);
            }
        } else {
            if (promisedId >= 0) {
                // not required by paxos but its 'nice' to return
                ret.setPromisedId(promisedId);
            }
            if (acceptedId >= 0) {
                // not required by paxos but its 'nice' to return
                ret.setAcceptedId(acceptedId);
                ret.setAcceptedValue(acceptedValue);
            }
            log.info("Ignoring prepare with id: {} (already promised: {})",
                    proposalId, promisedId);
            ret.setIgnored(true);
        }
        return ret;
    }

    /**
     * Phase 2b: Accept
     * If the proposal number is at least as high as any we've promised,
     * accept the proposal.
     */
    public synchronized AcceptResponse acceptRequest(long proposalId, String value) {
        // TODO write state to file after accept op in future version
        log.info("Received accept request with id: {}, value: {}",
                proposalId, value);

        if (proposalId >= promisedId) {
            // this code assumes proposer adheres to protocol and does not send
            // a different value for the same proposalId even though the code allows it
            promisedId = proposalId;
            acceptedId = proposalId;
            acceptedValue = value;
            log.info("Accepted id: {}, value: {}", proposalId, value);
            return AcceptResponse.accept(acceptedId, acceptedValue);
        } else {
            log.info("Ignoring accept with id: {} (promised: {})",
                    proposalId, promisedId);
            return AcceptResponse.ignore();
        }
    }

    public synchronized PaxosState getState() {
        return new PaxosState(promisedId, acceptedId, acceptedValue);
    }

    synchronized void setState(PaxosState state) {
        this.promisedId = state.getPromisedId();
        this.acceptedId = state.getAcceptedId();
        this.acceptedValue = state.getAcceptedValue();
    }
}
