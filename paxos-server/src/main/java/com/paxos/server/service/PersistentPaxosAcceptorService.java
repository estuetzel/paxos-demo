package com.paxos.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paxos.server.model.AcceptResponse;
import com.paxos.server.model.PaxosState;
import com.paxos.server.model.PromiseResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Wrapper around {@link PaxosAcceptorService} that persists state to a file.
 *
 */
@Service
public class PersistentPaxosAcceptorService implements IPaxosAcceptor {

    private static final Logger log = LoggerFactory.getLogger(PersistentPaxosAcceptorService.class);
    private static final String STATE_FILE_NAME = "paxos-state.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${paxos.server.id:0}")
    private int serverId;

    @Value("${paxos.server.data.dir}")
    private String dataDir;

    @Autowired
    private PaxosAcceptorService acceptorService;

    public PersistentPaxosAcceptorService() {

    }

    public PersistentPaxosAcceptorService(int serverId, String dataDir, PaxosAcceptorService acceptorService) {
        this.serverId = serverId;
        this.dataDir = dataDir;
        this.acceptorService = acceptorService;
    }

    @PostConstruct
    public void init() {
        loadStateFromFile();
    }

    void loadStateFromFile() {
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
                acceptorService.setState(state);
                log.info("Server {} loaded state from {}: {}}",
                        serverId, stateFile.getAbsolutePath(), state);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read state file: " + stateFile.getAbsolutePath(), e);
            }
        } else {
            log.info("Server {} state file does not exist, starting with fresh state: {}",
                    serverId, stateFile.getAbsolutePath());
        }
    }


    void persistState(PaxosState state) {
        File stateFile = Path.of(dataDir, String.valueOf(serverId), STATE_FILE_NAME).toFile();

        try {
            String stateString = objectMapper.writeValueAsString(state);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(stateFile, state);
            log.info("Server {} saved state to {}: state={}",
                    serverId, stateFile.getAbsolutePath(), stateString);
        } catch (IOException e) {
            log.error("Server {} failed to save state to {}", serverId, stateFile.getAbsolutePath(), e);
        }
    }

    public synchronized PromiseResponse prepare(long proposalId) {
        PaxosState before = acceptorService.getState();
        PromiseResponse ret = acceptorService.prepare(proposalId);
        PaxosState after = acceptorService.getState();
        if (!before.equals(after)) {
            persistState(after);
        }
        return ret;
    }


    public synchronized AcceptResponse acceptRequest(long proposalId, String value) {
        PaxosState before = acceptorService.getState();
        AcceptResponse ret = acceptorService.acceptRequest(proposalId, value);
        PaxosState after = acceptorService.getState();
        if (!before.equals(after)) {
            persistState(after);
        }
        return ret;
    }

    public synchronized PaxosState getState() {
        return acceptorService.getState();
    }
}
