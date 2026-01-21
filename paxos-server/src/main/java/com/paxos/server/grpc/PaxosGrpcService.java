package com.paxos.server.grpc;

import com.google.protobuf.Empty;
import com.paxos.server.grpc.proto.*;
import com.paxos.server.model.AcceptResponse;
import com.paxos.server.model.PaxosState;
import com.paxos.server.model.PromiseResponse;
import com.paxos.server.service.PaxosAcceptorService;
import com.paxos.server.service.PersistentPaxosAcceptorService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@GrpcService
public class PaxosGrpcService extends PaxosServiceGrpc.PaxosServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(PaxosGrpcService.class);

    @Value("${paxos.server.id:0}")
    private int serverId;

    private final PersistentPaxosAcceptorService acceptorService;

    public PaxosGrpcService(PersistentPaxosAcceptorService acceptorService) {
        this.acceptorService = acceptorService;
    }

    @Override
    public void prepare(PrepareRequest request, StreamObserver<PrepareResponse> responseObserver) {
        log.info("gRPC: Received prepare request with id: {}", request.getId());
        
        PromiseResponse result = acceptorService.prepare(request.getId());
        
        PrepareResponse.Builder responseBuilder = PrepareResponse.newBuilder()
                .setIgnored(result.isIgnored())
                .setPromisedId(result.getPromisedId());
        
        if (!result.isIgnored()) {
            if (result.getAcceptedId() != null) {
                responseBuilder.setAcceptedId(result.getAcceptedId());
                responseBuilder.setAcceptedValue(result.getAcceptedValue());
            }
        }
        log.debug("resp accepted grpc: has={}, val={}", responseBuilder.getHasAcceptedValue(), responseBuilder.getAcceptedValue());
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void acceptRequest(AcceptRequestMessage request, StreamObserver<com.paxos.server.grpc.proto.AcceptResponse> responseObserver) {
        log.info("gRPC: Received accept request with id: {}, value: {}", request.getId(), request.getValue());
        
        AcceptResponse result = acceptorService.acceptRequest(request.getId(), request.getValue());
        
        com.paxos.server.grpc.proto.AcceptResponse.Builder responseBuilder = 
                com.paxos.server.grpc.proto.AcceptResponse.newBuilder()
                        .setIgnored(result.isIgnored());
        
        if (!result.isIgnored()) {
            responseBuilder
                    .setAcceptedId(result.getAcceptedId())
                    .setAcceptedValue(result.getAcceptedValue());
        }
        
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void state(Empty request, StreamObserver<StateResponse> responseObserver) {
        PaxosState state = acceptorService.getState();

        StateResponse.Builder builder = StateResponse.newBuilder()
                .setPromisedId(state.getPromisedId())
                .setAcceptedId(state.getAcceptedId());

        if (state.getAcceptedValue() != null) {
            builder.setAcceptedValue(state.getAcceptedValue());
        }

        StateResponse resp = builder.build();

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void ping(PingRequest request, StreamObserver<PongResponse> responseObserver) {
        log.info("gRPC: Received ping request");
        
        PongResponse response = PongResponse.newBuilder()
                .setMessage("pong from server " + serverId)
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
