package com.paxos.server.integration;

import java.io.IOException;
import java.net.ServerSocket;

public class TestUtils {

    public static int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Could not find available port", e);
        }
    }
}
