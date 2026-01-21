package com.paxos.server.controller;

import com.paxos.server.model.AcceptResponse;
import com.paxos.server.model.PaxosState;
import com.paxos.server.model.PromiseResponse;
import com.paxos.server.service.CompositeAcceptorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for broadcasting prepare/send operations
 * to other nodes in the pool. In paxos, this is the responsibility of
 * proposer, not acceptor. This controller is largely here just for
 * convenience (to send one request to all nodes in the pool) and for the
 * purpose of demonstrating grpc inter-node communication as this is
 * an interview exercise.
 */
@RestController
@RequestMapping("/api/broadcast/paxos")
public class PaxosBroadcastRestController {

    private static final Logger log = LoggerFactory.getLogger(PaxosBroadcastRestController.class);

    private final CompositeAcceptorService acceptorService;

    public PaxosBroadcastRestController(CompositeAcceptorService acceptorService) {
        this.acceptorService = acceptorService;
    }

    /**
     * Phase 1: Prepare endpoint
     * Proposer sends prepare(n) to all acceptors
     * Acceptor responds with promise(n) or ignore
     */
    @PostMapping("/prepare")
    public ResponseEntity<Map<Integer, PromiseResponse>> prepare(@RequestParam int id) {
        log.info("REST: Received prepare request with id: {}", id);
        Map<Integer, PromiseResponse> responses = acceptorService.prepare(id);
        log.info("{} servers responded", responses.size());
        return ResponseEntity.ok(responses);
    }

    /**
     * Phase 2: Accept endpoint
     * Proposer sends accept(n, v) to acceptors
     * Acceptor responds with accept(n, v) or ignore
     */
    @PostMapping("/accept")
    public ResponseEntity<Map<Integer, AcceptResponse>> acceptRequest(
            @RequestParam int id,
            @RequestParam String value) {
        log.info("REST: Received accept request with id: {}, value: {}", id, value);
        Map<Integer, AcceptResponse> responses = acceptorService.acceptRequest(id, value);
        return ResponseEntity.ok(responses);
    }

    /**
     * Endpoint for getting paxos state
     */
    @GetMapping("/state")
    public ResponseEntity<Map<Integer, PaxosState>> state() {
        log.info("REST: Received state request");
        Map<Integer, PaxosState> states = acceptorService.state();
        return ResponseEntity.ok(states);
    }
}
