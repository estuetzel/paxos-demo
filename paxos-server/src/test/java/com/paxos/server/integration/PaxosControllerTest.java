package com.paxos.server.integration;

import com.paxos.server.model.AcceptResponse;
import com.paxos.server.model.PaxosState;
import com.paxos.server.model.PromiseResponse;
import com.paxos.server.service.PersistentPaxosAcceptorService;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.paxos.server.integration.TestUtils.findAvailablePort;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PaxosControllerTest {

    @LocalServerPort
    private int httpPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @TempDir
    static Path tempDir;

    @Autowired
    private PersistentPaxosAcceptorService pservice;

    private static int grpcPort;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        grpcPort = findAvailablePort();
        registry.add("paxos.server.data.dir", () -> tempDir.toString());
        registry.add("paxos.server.id", () -> "1");
        registry.add("grpc.server.port", () -> grpcPort);
    }

    @AfterEach
    void cleanupDataDir() throws IOException {
        // Clean up the server's data subdirectory before each test
        Path serverDataDir = tempDir.resolve("1");
        if (Files.exists(serverDataDir)) {
            FileUtils.deleteDirectory(serverDataDir.toFile());
        }
        serverDataDir.toFile().mkdir();
    }

    @Test
    void testStateWithNoOperations_matchesInitialState() {
        // no operations performed yet

        // When: requesting state
        ResponseEntity<PaxosState> response = restTemplate.getForEntity(
                "/api/paxos/state",
                PaxosState.class
        );

        // Then: state matches initial values
        assertTrue(response.getStatusCode().is2xxSuccessful());
        PaxosState state = response.getBody();
        assertNotNull(state);
        assertEquals(-1, state.getPromisedId());
        assertEquals(-1, state.getAcceptedId());
        assertNull(state.getAcceptedValue());
    }

    @Test
    void testPrepare_thenVerifyState() {
        // When: sending prepare request with id=5
        ResponseEntity<PromiseResponse> prepareResponse = restTemplate.postForEntity(
                "/api/paxos/prepare?id=5",
                null,
                PromiseResponse.class
        );

        // Then: prepare is successful
        assertTrue(prepareResponse.getStatusCode().is2xxSuccessful());
        PromiseResponse promise = prepareResponse.getBody();
        assertNotNull(promise);
        assertFalse(promise.isIgnored());
        assertEquals(5L, promise.getPromisedId());

        // And: state reflects the promise
        ResponseEntity<PaxosState> stateResponse = restTemplate.getForEntity(
                "/api/paxos/state",
                PaxosState.class
        );

        assertTrue(stateResponse.getStatusCode().is2xxSuccessful());
        PaxosState state = stateResponse.getBody();
        assertNotNull(state);
        assertEquals(5, state.getPromisedId());
        assertEquals(-1, state.getAcceptedId());
        assertNull(state.getAcceptedValue());
    }

    @Test
    void testPrepareThenIgnored() {

        // When: sending prepare request with id=5
        ResponseEntity<PromiseResponse> prepareResponse = restTemplate.postForEntity(
                "/api/paxos/prepare?id=5",
                null,
                PromiseResponse.class
        );

        // Then: prepare is successful
        assertTrue(prepareResponse.getStatusCode().is2xxSuccessful());
        PromiseResponse promise = prepareResponse.getBody();
        assertNotNull(promise);
        assertFalse(promise.isIgnored());
        assertEquals(5L, promise.getPromisedId());


        prepareResponse = restTemplate.postForEntity(
                "/api/paxos/prepare?id=3",
                null,
                PromiseResponse.class
        );

        // Then: prepare w/ lower ID is ignored
        assertTrue(prepareResponse.getStatusCode().is2xxSuccessful());
        promise = prepareResponse.getBody();
        assertNotNull(promise);
        assertTrue(promise.isIgnored());
        // but we see previous promise bc acceptor is 'nice'
        assertEquals(5L, promise.getPromisedId());
    }

    @Test
    void testPrepareAndAcceptThenVerifyState() {
        // When: sending prepare request with id=10
        ResponseEntity<PromiseResponse> prepareResponse = restTemplate.postForEntity(
                "/api/paxos/prepare?id=10",
                null,
                PromiseResponse.class
        );

        // Then: prepare is successful
        assertTrue(prepareResponse.getStatusCode().is2xxSuccessful());
        PromiseResponse promise = prepareResponse.getBody();
        assertNotNull(promise);
        assertFalse(promise.isIgnored());
        assertEquals(10L, promise.getPromisedId());

        // When: sending accept request with id=10 and value="test-value"
        ResponseEntity<AcceptResponse> acceptResponse = restTemplate.postForEntity(
                "/api/paxos/accept?id=10&value=test-value",
                null,
                AcceptResponse.class
        );

        // Then: accept is successful
        assertTrue(acceptResponse.getStatusCode().is2xxSuccessful());
        AcceptResponse accept = acceptResponse.getBody();
        assertNotNull(accept);
        assertFalse(accept.isIgnored());
        assertEquals(10L, accept.getAcceptedId());
        assertEquals("test-value", accept.getAcceptedValue());

        // And: state reflects both the promise and the accepted value
        ResponseEntity<PaxosState> stateResponse = restTemplate.getForEntity(
                "/api/paxos/state",
                PaxosState.class
        );

        assertTrue(stateResponse.getStatusCode().is2xxSuccessful());
        PaxosState state = stateResponse.getBody();
        assertNotNull(state);
        assertEquals(10, state.getPromisedId());
        assertEquals(10, state.getAcceptedId());
        assertEquals("test-value", state.getAcceptedValue());
    }
}
