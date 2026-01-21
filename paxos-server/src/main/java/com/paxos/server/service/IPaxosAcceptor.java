package com.paxos.server.service;

import com.paxos.server.model.AcceptResponse;
import com.paxos.server.model.PaxosState;
import com.paxos.server.model.PromiseResponse;
import org.springframework.stereotype.Service;

/**
 * Interface for a Paxos acceptor role.
 */
@Service
public interface IPaxosAcceptor {

    /**
     * Paxos Phase 1b: Prepare/Promise
     */
    PromiseResponse prepare(long proposalId);

    /**
     * Paxos Phase 2b: Accept
     */
    AcceptResponse acceptRequest(long proposalId, String value);

    /**
     * Get the state of this acceptor
     */
    PaxosState getState();

}
