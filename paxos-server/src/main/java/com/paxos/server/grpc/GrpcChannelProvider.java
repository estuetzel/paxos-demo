package com.paxos.server.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


/**
 * Provides access to a channel for a specified grpc server, efficiently reusing/pooling
 * connections.
 */
@Service
public class GrpcChannelProvider implements SmartInitializingSingleton {



    private static final Logger log = LoggerFactory.getLogger(GrpcChannelProvider.class);

    @Value("${paxos.server.id:0}")
    private int serverId;

    @Value("${paxos.server.grpc.base.port:9090}")
    private int baseGrpcPort;

    @Value("${paxos.server.count}")
    private int serverCount;

    private Map<Integer, ManagedChannel> channels = new HashMap<>();

    @Override
    public void afterSingletonsInstantiated() {
        for (int i = 1; i <= serverCount; i++) {
            if (i == serverId) {
                continue;
            }
            int grpcPort = baseGrpcPort + i;
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", grpcPort)
                    .usePlaintext()
                    .build();
            channels.put(i, channel);
        }
        log.info("grpc channels initiated.");
    }

    public ManagedChannel getChannel(int i) {
        ManagedChannel ret = channels.get(i);
        if (ret == null) {
            throw new IllegalStateException("No channel for server " + i);
        }
        return ret;
    }

    @EventListener
    public void onShutdown(ContextClosedEvent event) {
        // TODO safely shutdown tcp connections
    }
}
