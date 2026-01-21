package com.paxos.server.integration;

import com.paxos.server.PaxosServerApplication;
import com.paxos.server.model.PaxosState;
import com.paxos.server.model.PromiseResponse;
import com.paxos.server.service.PersistentPaxosAcceptorService;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.paxos.server.integration.TestUtils.findAvailablePort;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Server to server integration test.
 *
 * This is not a comprehensive set of integration tests over the grpc service. It is
 * just here to demonstrate testing inter-server communication by launching multiple
 * server instances (via spring context initialization).
 *
 */
public class MultiServerIntegrationTest {

    private static class TestServerContext {
        ConfigurableApplicationContext sprintCtx;
        int httpPort;
        String baseUrl;

        TestServerContext(ConfigurableApplicationContext sprintCtx, int httpPort) {
            this.sprintCtx = sprintCtx;
            this.httpPort = httpPort;
            this.baseUrl = String.format("http://localhost:%d", httpPort);
        }
    }

    @Test
    void testGetStateProposerController() throws Exception {
        List<TestServerContext> contexts = new ArrayList<>();
        List<Path> tempDirs = new ArrayList<>();
        int serverCount = 3;

        int baseGrpcPort = findAvailablePort();
        for (int i = 1; i <= serverCount; i++) {
            // this is just a hack while the grpc port
            // is configured in an overly simple way (sequence using serverCount)
            // will fix when containerized using docker
            findAvailablePort();
        }

        System.out.println(baseGrpcPort);

        try {
            for (int i = 1; i <= serverCount; i++) {
                // Create a unique temp dir for each server
                Path tempDir = Files.createTempDirectory("paxos-server-" + i);
                tempDirs.add(tempDir);

                int httpPort = findAvailablePort();
                int grpcPort = baseGrpcPort + i; // bad -- assuming it is free



                // Launch a fully independent Spring Boot context
                ConfigurableApplicationContext ctx = new SpringApplicationBuilder(PaxosServerApplication.class)
                        .properties()
                        .run(
                                "--paxos.server.id=" + i,
                                "--paxos.server.data.dir=" + tempDir.toAbsolutePath(),
                                "--grpc.server.port=" + grpcPort,
                                "--server.port=" + httpPort,
                                "--paxos.server.grpc.base.port=" + baseGrpcPort,
                                "--paxos.server.count=" + serverCount
                        );

                contexts.add(new TestServerContext(ctx, httpPort));

                // Verify the bean is initialized correctly
                PersistentPaxosAcceptorService service =
                        ctx.getBean(PersistentPaxosAcceptorService.class);
                assertNotNull(service);
                System.out.println("Server " + i + " started with data dir: " + tempDir);
            }

            // At this point, all 3 servers are independent and can talk to each other
            TestRestTemplate httpTemplate = new TestRestTemplate();

            // Send a prepare message to each via http
            for (int i = 1; i <= serverCount; i++) {
                ResponseEntity<PromiseResponse> resp = httpTemplate.postForEntity(
                        String.format("%s/api/paxos/prepare?id=%d", contexts.get(i-1).baseUrl, i), null, PromiseResponse.class
                );
                assertTrue(resp.getStatusCode().is2xxSuccessful(), resp.toString());
            }

            // Now trigger a grpc call from node 1 to all others
            // by hitting the proposer controller
            ResponseEntity<Map<Integer, PaxosState>> resp =
                    httpTemplate.exchange(
                            String.format("%s/api/broadcast/paxos/state", contexts.get(0).baseUrl),
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<Map<Integer, PaxosState>>() {}
                    );
            assertTrue(resp.getStatusCode().is2xxSuccessful());
            Map<Integer, PaxosState> states = resp.getBody();
            assertEquals(states.size(), serverCount);

            for (int i = 1; i <= serverCount; i++) {
                assertTrue(states.containsKey(i));
                PaxosState state = states.get(i);
                assertEquals(i, state.getPromisedId());
            }


        } finally {
            // Cleanup: stop all servers and delete temp dirs
            for (TestServerContext ctx : contexts) {
                ctx.sprintCtx.close();
            }
            for (Path dir : tempDirs) {
                FileUtils.deleteDirectory(dir.toFile());
            }
        }
    }
}
