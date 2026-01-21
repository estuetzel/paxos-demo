package com.paxos.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paxos.server.model.AcceptResponse;
import com.paxos.server.model.PaxosState;
import com.paxos.server.model.PromiseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Generated w/ Opus 4.5 using this prompt:
 * Add some basic tests to this test class. each test verifies the file persisted matches the expected state
 * 1. load/init from fresh state
 * 2. load/init from existing state
 * 3. state file matches expected after prepare call
 * 4. state file matches expected after acceptRequest call
 * 5. tests that verify state file matches expected and isnt modified after idempotent prepare and accept calls
 */
class PersistentPaxosAcceptorServiceTest {

    private static final int SERVER_ID = 1;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tmpDir;

    private Path getStateFilePath() {
        return tmpDir.resolve(String.valueOf(SERVER_ID)).resolve("paxos-state.json");
    }

    private PaxosState readStateFile() throws IOException {
        return objectMapper.readValue(getStateFilePath().toFile(), PaxosState.class);
    }

    private void writeStateFile(PaxosState state) throws IOException {
        File dir = tmpDir.resolve(String.valueOf(SERVER_ID)).toFile();
        dir.mkdirs();
        objectMapper.writeValue(getStateFilePath().toFile(), state);
    }

    @Test
    @DisplayName("init from fresh state creates directory but no state file")
    void testInitFreshState() {
        PaxosAcceptorService inner = new PaxosAcceptorService();
        PersistentPaxosAcceptorService svc = new PersistentPaxosAcceptorService(SERVER_ID, tmpDir.toString(), inner);
        svc.init();

        // Directory should exist
        assertTrue(tmpDir.resolve(String.valueOf(SERVER_ID)).toFile().exists());
        // State file should not exist (no operations performed yet)
        assertFalse(getStateFilePath().toFile().exists());
        // State should be default values
        PaxosState state = svc.getState();
        assertEquals(-1, state.getPromisedId());
        assertEquals(-1, state.getAcceptedId());
        assertNull(state.getAcceptedValue());
    }

    @Test
    @DisplayName("init from existing state file loads persisted state")
    void testInitExistingState() throws IOException {
        PaxosState existingState = new PaxosState(100, 50, "existing-value");
        writeStateFile(existingState);

        PaxosAcceptorService inner = new PaxosAcceptorService();
        PersistentPaxosAcceptorService svc = new PersistentPaxosAcceptorService(SERVER_ID, tmpDir.toString(), inner);
        svc.init();

        PaxosState loadedState = svc.getState();
        assertEquals(existingState, loadedState);
    }

    @Test
    @DisplayName("prepare call persists expected state to file")
    void testPrepareCallPersistsState() throws IOException {
        PaxosAcceptorService inner = new PaxosAcceptorService();
        PersistentPaxosAcceptorService svc = new PersistentPaxosAcceptorService(SERVER_ID, tmpDir.toString(), inner);
        svc.init();

        svc.prepare(42);

        PaxosState persistedState = readStateFile();
        assertEquals(42, persistedState.getPromisedId());
        assertEquals(-1, persistedState.getAcceptedId());
        assertNull(persistedState.getAcceptedValue());
    }

    @Test
    @DisplayName("acceptRequest call persists expected state to file")
    void testAcceptRequestCallPersistsState() throws IOException {
        PaxosAcceptorService inner = new PaxosAcceptorService();
        PersistentPaxosAcceptorService svc = new PersistentPaxosAcceptorService(SERVER_ID, tmpDir.toString(), inner);
        svc.init();

        // First prepare, then accept
        svc.prepare(100);
        svc.acceptRequest(100, "test-value");

        PaxosState persistedState = readStateFile();
        assertEquals(100, persistedState.getPromisedId());
        assertEquals(100, persistedState.getAcceptedId());
        assertEquals("test-value", persistedState.getAcceptedValue());
    }

    @Test
    @DisplayName("repeated prepare with same proposal ID does not modify state file")
    void testRepeatedPrepareDoesNotModifyFile() throws IOException, InterruptedException {
        PaxosAcceptorService inner = new PaxosAcceptorService();
        PersistentPaxosAcceptorService svc = new PersistentPaxosAcceptorService(SERVER_ID, tmpDir.toString(), inner);
        svc.init();

        // Initial prepare
        svc.prepare(100);

        File stateFile = getStateFilePath().toFile();
        long lastModifiedBefore = stateFile.lastModified();
        PaxosState stateBefore = readStateFile();

        // Small delay to ensure timestamp would change if file is modified
        Thread.sleep(10);

        // Same prepare again should be ignored (not > promisedId)
        PromiseResponse response = svc.prepare(100);
        assertTrue(response.isIgnored());

        // File should not be modified
        assertEquals(lastModifiedBefore, stateFile.lastModified());
        assertEquals(stateBefore, readStateFile());
    }

    @Test
    @DisplayName("repeated acceptRequest with same proposal ID and value does not modify state file")
    void testRepeatedAcceptDoesNotModifyFile() throws IOException, InterruptedException {
        PaxosAcceptorService inner = new PaxosAcceptorService();
        PersistentPaxosAcceptorService svc = new PersistentPaxosAcceptorService(SERVER_ID, tmpDir.toString(), inner);
        svc.init();

        // Initial prepare and accept
        svc.prepare(100);
        svc.acceptRequest(100, "test-value");

        File stateFile = getStateFilePath().toFile();
        long lastModifiedBefore = stateFile.lastModified();
        PaxosState stateBefore = readStateFile();

        // Small delay to ensure timestamp would change if file is modified
        Thread.sleep(10);

        // Same accept again - state unchanged so file should not be modified
        AcceptResponse response = svc.acceptRequest(100, "test-value");
        assertFalse(response.isIgnored());

        // File should not be modified (state is identical)
        assertEquals(lastModifiedBefore, stateFile.lastModified());
        assertEquals(stateBefore, readStateFile());
    }
}