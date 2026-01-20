package com.paxos.server.controller;

import com.paxos.server.model.AcceptResponse;
import com.paxos.server.model.PaxosState;
import com.paxos.server.model.PromiseResponse;
import com.paxos.server.service.PaxosAcceptorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for paxos operations on this host.
 */
@RestController
@RequestMapping("/api/paxos")
public class PaxosRestController {

    private static final Logger log = LoggerFactory.getLogger(PaxosRestController.class);

    private final PaxosAcceptorService acceptorService;

    public PaxosRestController(PaxosAcceptorService acceptorService) {
        this.acceptorService = acceptorService;
    }

    /**
     * Phase 1: Prepare endpoint
     * Proposer sends prepare(n) to acceptor
     * Acceptor responds with promise(n) or ignore
     */
    @PostMapping("/prepare")
    public ResponseEntity<PromiseResponse> prepare(@RequestParam int id) {
        log.info("REST: Received prepare request with id: {}", id);
        PromiseResponse response = acceptorService.prepare(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Phase 2: Accept endpoint
     * Proposer sends accept(n, v) to acceptors
     * Acceptor responds with accept(n, v) or ignore
     */
    @PostMapping("/accept")
    public ResponseEntity<AcceptResponse> acceptRequest(
            @RequestParam int id,
            @RequestParam String value) {
        log.info("REST: Received accept request with id: {}, value: {}", id, value);
        AcceptResponse response = acceptorService.acceptRequest(id, value);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint for getting paxos state
     */
    @GetMapping("/state")
    public ResponseEntity<PaxosState> state() {
        log.info("REST: Received state request");
        PaxosState state = acceptorService.getState();
        return ResponseEntity.ok(state);
    }
}
