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
 */
@Service
public class PaxosAcceptorService {

    private static final Logger log = LoggerFactory.getLogger(PaxosAcceptorService.class);
    private static final String STATE_FILE_NAME = "paxos-state.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${paxos.server.id:0}")
    private int serverId;

    @Value("${paxos.server.data.dir}")
    private String dataDir;

    // Highest proposal number promised
    private long promisedId = -1;
    
    // Highest proposal number accepted
    private long acceptedId = -1;
    
    // Value associated with the accepted proposal
    private String acceptedValue = null;

    @PostConstruct
    public void init() {
        if (dataDir == null || dataDir.isBlank()) {
            throw new IllegalStateException("paxos.server.data.dir must be non-empty");
        }

        File dataDirFile = Path.of(dataDir, String.valueOf(serverId)).toFile();
        if (!dataDirFile.exists()) {
            dataDirFile.mkdirs();
            log.info("Created missing data: dir {}", dataDirFile.getAbsolutePath());
            return;
        }

        File stateFile = new File(dataDirFile, STATE_FILE_NAME);

        if (stateFile.exists()) {
            try {
                PaxosState state = objectMapper.readValue(stateFile, PaxosState.class);
                this.promisedId = state.getPromisedId();
                this.acceptedId = state.getAcceptedId();
                this.acceptedValue = state.getAcceptedValue();
                log.info("Server {} loaded state from {}: promisedId={}, acceptedId={}, acceptedValue={}",
                        serverId, stateFile.getAbsolutePath(), promisedId, acceptedId, acceptedValue);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read state file: " + stateFile.getAbsolutePath(), e);
            }
        } else {
            log.info("Server {} state file does not exist, starting with fresh state: {}",
                    serverId, stateFile.getAbsolutePath());
        }
    }

    @PreDestroy
    public void shutdown() {
        // TODO investigate if this hook can be called while http or rpc calls
        // are in flight still
        File serverDir = Path.of(dataDir, String.valueOf(serverId)).toFile();
        if (!serverDir.exists()) {
            serverDir.mkdirs();
        }

        File stateFile = new File(serverDir, STATE_FILE_NAME);
        PaxosState state = new PaxosState(promisedId, acceptedId, acceptedValue);

        try {
            String stateString = objectMapper.writeValueAsString(state);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(stateFile, state);
            log.info("Server {} saved state to {}: state={}",
                    serverId, stateFile.getAbsolutePath(), stateString);
        } catch (IOException e) {
            log.error("Server {} failed to save state to {}", serverId, stateFile.getAbsolutePath(), e);
        }
    }

    /**
     * Phase 1b: Prepare/Promise
     * If the proposal number is higher than any we've seen, promise not to accept
     * any proposal with a lower number.
     */
    public synchronized PromiseResponse prepare(long proposalId) {
        // TODO write state to file after prepare op in future version
        log.info("Server {} received prepare request with id: {}", serverId, proposalId);
        PromiseResponse ret = new PromiseResponse();
        ret.setPromisedId(proposalId);
        if (proposalId > promisedId) {
            promisedId = proposalId;
            log.info("Server {} promising for id: {}", serverId, proposalId);
            // when promising proposalId, return previously highest acceptedId and value
            if (acceptedId >= 0) {
                ret.setAcceptedId(acceptedId);
                ret.setAcceptedValue(acceptedValue);
            }
        } else {
            log.info("Server {} ignoring prepare with id: {} (already promised: {})",
                    serverId, proposalId, promisedId);
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
        log.info("Server {} received accept request with id: {}, value: {}",
                serverId, proposalId, value);

        if (proposalId >= promisedId) {
            // possible bug -- acceptedValue cannot change for the same proposalId
            // requires some smart/correct behavior by proposer
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
    }

    public int getServerId() {
        return serverId;
    }

    public synchronized PaxosState getState() {
        return new PaxosState(promisedId, acceptedId, acceptedValue);
    }
}
