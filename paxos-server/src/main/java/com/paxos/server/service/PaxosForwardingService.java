package com.paxos.server.service;

import com.google.protobuf.Empty;
import com.paxos.server.grpc.GrpcChannelProvider;
import com.paxos.server.grpc.proto.AcceptRequestMessage;
import com.paxos.server.grpc.proto.PaxosServiceGrpc;
import com.paxos.server.grpc.proto.PrepareRequest;
import com.paxos.server.grpc.proto.PrepareResponse;
import com.paxos.server.model.AcceptResponse;
import com.paxos.server.model.PaxosState;
import com.paxos.server.model.PromiseResponse;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Paxos forwarding service.
 * Sends prepare/accept request to other participants in consensus pool
 * TODO consider stopping prepare/accept requests once quorum is reached
 */
@Service
public class PaxosForwardingService {

    private static final Logger log = LoggerFactory.getLogger(PaxosForwardingService.class);

    @Value("${paxos.server.id:0}")
    private int serverId;

    @Value("${paxos.server.count}")
    private int serverCount;

    @Autowired
    private GrpcChannelProvider grpcChannelProvider;

    /**
     * Send prepare request to other servers in pool
     */
    public Map<Integer, PromiseResponse> prepare(long proposalId) {
        Map<Integer, PromiseResponse> responses = new HashMap<>();
        
        for (int i = 1; i <= serverCount; i++) {
            if (i == serverId) {
                continue;
            }
            
            try {
                ManagedChannel channel = grpcChannelProvider.getChannel(i);
                
                PaxosServiceGrpc.PaxosServiceBlockingStub stub = PaxosServiceGrpc.newBlockingStub(channel);
                
                PrepareRequest request = PrepareRequest.newBuilder()
                        .setId(proposalId)
                        .build();
                
                PrepareResponse grpcResponse = stub.prepare(request);
                
                PromiseResponse promiseResponse = new PromiseResponse();
                promiseResponse.setIgnored(grpcResponse.getIgnored());
                promiseResponse.setPromisedId(grpcResponse.getPromisedId());
                if (grpcResponse.getHasAcceptedValue()) {
                    promiseResponse.setAcceptedId(grpcResponse.getAcceptedId());
                    promiseResponse.setAcceptedValue(grpcResponse.getAcceptedValue());
                }
                
                responses.put(i, promiseResponse);
                log.info("Received prepare response from server {}: ignored={}", i, promiseResponse.isIgnored());
                
            } catch (Exception e) {
                log.warn("Failed to send prepare request to server {}: {}", i, e.getMessage());
            }
        }
        log.info("Total prepared responses: {}", responses.size());
        return responses;
    }

    /**
     * Send accept request to other servers in pool
     */
    public Map<Integer, AcceptResponse> acceptRequest(long proposalId, String value) {
        Map<Integer, AcceptResponse> responses = new HashMap<>();

        for (int i = 1; i <= serverCount; i++) {
            if (i == serverId) {
                continue;
            }

            try {
                ManagedChannel channel = grpcChannelProvider.getChannel(i);

                PaxosServiceGrpc.PaxosServiceBlockingStub stub = PaxosServiceGrpc.newBlockingStub(channel);

                AcceptRequestMessage request = AcceptRequestMessage.newBuilder()
                        .setId(proposalId)
                        .setValue(value)
                        .build();

                com.paxos.server.grpc.proto.AcceptResponse grpcResponse = stub.acceptRequest(request);

                AcceptResponse acceptResp = new AcceptResponse();
                acceptResp.setIgnored(grpcResponse.getIgnored());
                acceptResp.setAcceptedId(grpcResponse.getAcceptedId());
                acceptResp.setAcceptedValue(grpcResponse.getAcceptedValue());

                responses.put(i, acceptResp);
                log.info("Received accept response from server {}: ignored={}", i, acceptResp.isIgnored());

            } catch (Exception e) {
                log.warn("Failed to send accept request to server {}: {}", i, e.getMessage());
            }
        }
        log.info("Total accept responses: {}", responses.size());
        return responses;
    }

    public Map<Integer, PaxosState> getState() {
        Map<Integer, PaxosState> states = new HashMap<>();

        for (int i = 1; i <= serverCount; i++) {
            if (i == serverId) {
                continue;
            }

            try {
                ManagedChannel channel = grpcChannelProvider.getChannel(i);

                PaxosServiceGrpc.PaxosServiceBlockingStub stub = PaxosServiceGrpc.newBlockingStub(channel);

                Empty request = Empty.newBuilder()
                        .build();

                com.paxos.server.grpc.proto.StateResponse grpcResponse = stub.state(request);

                PaxosState state = new PaxosState(
                        grpcResponse.getPromisedId(),
                        grpcResponse.getAcceptedId(),
                        grpcResponse.hasAcceptedValue() ? grpcResponse.getAcceptedValue() : null
                );

                states.put(i, state);
                log.debug("Received state from server {}: {}", i, state);

            } catch (Exception e) {
                log.warn("Failed to send state request to server {}: {}", i, e.getMessage());
            }
        }
        log.info("Total state responses: {}", states.size());
        return states;

    }
}
