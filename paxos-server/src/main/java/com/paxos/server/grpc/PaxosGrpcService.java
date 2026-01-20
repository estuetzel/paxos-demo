package com.paxos.server.grpc;

import com.paxos.server.grpc.proto.*;
import com.paxos.server.model.AcceptResponse;
import com.paxos.server.model.PromiseResponse;
import com.paxos.server.service.PaxosAcceptorService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class PaxosGrpcService extends PaxosServiceGrpc.PaxosServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(PaxosGrpcService.class);

    private final PaxosAcceptorService acceptorService;

    public PaxosGrpcService(PaxosAcceptorService acceptorService) {
        this.acceptorService = acceptorService;
    }

    @Override
    public void prepare(PrepareRequest request, StreamObserver<PrepareResponse> responseObserver) {
        log.info("gRPC: Received prepare request with id: {}", request.getId());
        
        PromiseResponse result = acceptorService.prepare(request.getId());
        
        PrepareResponse.Builder responseBuilder = PrepareResponse.newBuilder()
                .setIgnored(result.isIgnored());
        
        if (!result.isIgnored()) {
            responseBuilder.setPromisedId(result.getId());
        }
        
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
    public void ping(PingRequest request, StreamObserver<PongResponse> responseObserver) {
        log.info("gRPC: Received ping request");
        
        PongResponse response = PongResponse.newBuilder()
                .setMessage("pong from server " + acceptorService.getServerId())
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
