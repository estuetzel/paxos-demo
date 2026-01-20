package com.paxos.server.service;

import com.paxos.server.model.AcceptResponse;
import com.paxos.server.model.PaxosState;
import com.paxos.server.model.PromiseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acts as a combined service for the current host and other hosts in the
 * pool, communicating via grpc. Not typical in paxos because acceptors don't
 * generally send message to other acceptors, but it is here for demonstration
 * and convenience.
 */
@Service
public class CompositeAcceptorService {

    private static final Logger log = LoggerFactory.getLogger(CompositeAcceptorService.class);

    @Value("${paxos.server.id:0}")
    private int serverId;
    @Value("${paxos.server.count}")
    private int serverCount;

    @Autowired
    private PaxosAcceptorService internalService;

    @Autowired
    private PaxosForwardingService remoteService;


    /**
     * Combined paxos prepare operation for this host and remote hosts in consensus pool.
     * Return a map of server id to prepare responses.
     */
    public Map<Integer, PromiseResponse> prepare(long proposalId) {

        Map<Integer, PromiseResponse> ret = new HashMap<>();
        List<Integer> serversIgnored = new ArrayList<>();

        PromiseResponse internalResp = internalService.prepare(proposalId);
        if (!internalResp.isIgnored()) {
            ret.put(serverId, internalResp);
        } else {
            serversIgnored.add(serverId);
        }

        Map<Integer, PromiseResponse> remoteResponses = remoteService.prepare(proposalId);
        log.debug("remote prepare responses: {}", remoteResponses);

        for (int i = 1; i <= serverCount; i++) {
            if (i == serverId) {
                continue;
            }
            if (remoteResponses.containsKey(i)) {
                PromiseResponse resp = remoteResponses.get(i);
                if (!resp.isIgnored()) {
                    ret.put(i, resp);
                } else {
                    serversIgnored.add(serverId);
                }
            }
        }

        if (!serversIgnored.isEmpty()) {
            log.info("Prepare responses ignored for servers: {}", serversIgnored);
        }

        return ret;
    }

    /**
     * Combined paxos accept operation for this host and remote hosts in consensus pool.
     * Return a map of server id to accept responses.
     */
    public Map<Integer, AcceptResponse> accept(long proposalId, String value) {

        Map<Integer, AcceptResponse> ret = new HashMap<>();
        List<Integer> serversIgnored = new ArrayList<>();

        AcceptResponse internalResp = internalService.acceptRequest(proposalId, value);
        if (!internalResp.isIgnored()) {
            ret.put(serverId, internalResp);
        } else {
            serversIgnored.add(serverId);
        }

        Map<Integer, AcceptResponse> remoteResponses = remoteService.acceptRequest(proposalId, value);
        log.debug("remote accept responses: {}", remoteResponses);

        for (int i = 1; i <= serverCount; i++) {
            if (i == serverId) {
                continue;
            }
            if (remoteResponses.containsKey(i)) {
                AcceptResponse resp = remoteResponses.get(i);
                if (!resp.isIgnored()) {
                    ret.put(i, resp);
                } else {
                    serversIgnored.add(serverId);
                }
            }
        }

        if (!serversIgnored.isEmpty()) {
            log.info("Accept responses ignored for servers: {}", serversIgnored);
        }

        return ret;
    }

    public Map<Integer, PaxosState> state() {
        Map<Integer, PaxosState> ret = new HashMap<>();
        PaxosState internalState = internalService.getState();
        ret.put(serverId, internalState);
        Map<Integer, PaxosState> remoteStates = remoteService.getState();
        log.debug("remote states: {}", remoteStates);
        ret.putAll(remoteStates);
        return ret;
    }
}
