package com.projects.logstore.replication;

import com.google.protobuf.ByteString;
import com.projects.logstore.config.ClusterProperties;
import com.projects.logstore.dto.ClusterStatusDTO;
import com.projects.logstore.dto.ReplicationRecordDTO;
import com.projects.logstore.dto.ReplicationResultDTO;
import com.projects.logstore.proto.AppendRequest;
import com.projects.logstore.proto.AppendResponse;
import com.projects.logstore.proto.ClusterStatusRequest;
import com.projects.logstore.proto.ClusterStatusResponse;
import com.projects.logstore.proto.FetchFromOffsetRequest;
import com.projects.logstore.proto.HealthRequest;
import com.projects.logstore.proto.HealthResponse;
import com.projects.logstore.proto.LogStoreServiceGrpc;
import com.projects.logstore.proto.PeerStatus;
import com.projects.logstore.proto.ReadRequest;
import com.projects.logstore.proto.ReadResponse;
import com.projects.logstore.proto.ReplicationResponse;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class GrpcReplicationServer implements SmartLifecycle {
    private final ClusterProperties clusterProperties;
    private final ReplicationManager replicationManager;
    private Server server;
    private boolean running;

    public GrpcReplicationServer(ClusterProperties clusterProperties, ReplicationManager replicationManager) {
        this.clusterProperties = clusterProperties;
        this.replicationManager = replicationManager;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        if (!clusterProperties.isGrpcEnabled()) {
            log.info("LogStore gRPC replication server disabled");
            return;
        }
        try {
            server = NettyServerBuilder.forPort(clusterProperties.getGrpcPort())
                    .addService(new Service())
                    .build()
                    .start();
            running = true;
            log.info("LogStore gRPC replication server listening on {}", clusterProperties.getGrpcPort());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start LogStore gRPC replication server", ex);
        }
    }

    @Override
    @PreDestroy
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private final class Service extends LogStoreServiceGrpc.LogStoreServiceImplBase {
        @Override
        public void replicate(com.projects.logstore.proto.ReplicationRecord request, StreamObserver<ReplicationResponse> responseObserver) {
            ReplicationRecordDTO dto = new ReplicationRecordDTO();
            dto.setStream(request.getStream());
            dto.setKey(request.getKey());
            dto.setValue(request.getValue().toString(StandardCharsets.UTF_8));
            dto.setOffset(request.getOffset());
            dto.setTimestamp(request.getTimestamp());
            ReplicationResultDTO result = replicationManager.appendFromLeader(dto);
            responseObserver.onNext(ReplicationResponse.newBuilder()
                    .setSuccess(result.isSuccess())
                    .setOffset(result.getOffset())
                    .setExpectedOffset(result.getExpectedOffset())
                    .setMessage(result.getMessage() == null ? "" : result.getMessage())
                    .build());
            responseObserver.onCompleted();
        }

        @Override
        public void fetchFromOffset(FetchFromOffsetRequest request, StreamObserver<ReadResponse> responseObserver) {
            var fetched = replicationManager.fetchFromOffset(request.getTabletId(), request.getOffset(), request.getLimit());
            ReadResponse.Builder response = ReadResponse.newBuilder().setNextOffset(fetched.getNextOffset());
            fetched.getRecords().forEach(record -> response.addRecords(com.projects.logstore.proto.LogRecord.newBuilder()
                    .setStream(record.getStream())
                    .setKey(record.getKey())
                    .setValue(ByteString.copyFrom(record.getValue().getBytes(StandardCharsets.UTF_8)))
                    .setOffset(record.getOffset())
                    .setTimestamp(record.getTimestamp())
                    .build()));
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }

        @Override
        public void clusterStatus(ClusterStatusRequest request, StreamObserver<ClusterStatusResponse> responseObserver) {
            ClusterStatusDTO status = new ClusterStatusDTO();
            status.setNodeId(clusterProperties.getNodeId());
            status.setLeader(clusterProperties.isLeader());
            status.setAckMode(clusterProperties.getAckMode().name());
            status.setLatestOffset(replicationManager.latestOffset());
            status.setCommitOffset(replicationManager.commitOffset());
            ClusterStatusResponse.Builder response = ClusterStatusResponse.newBuilder()
                    .setNodeId(status.getNodeId())
                    .setLeader(status.isLeader())
                    .setAckMode(status.getAckMode())
                    .setLatestOffset(status.getLatestOffset())
                    .setCommitOffset(status.getCommitOffset());
            if (clusterProperties.isLeader()) {
                replicationManager.peerStatuses().forEach(peer -> response.addPeers(PeerStatus.newBuilder()
                        .setPeer(peer.getPeer())
                        .setHealthy(peer.isHealthy())
                        .setLatestOffset(peer.getLatestOffset())
                        .setLag(peer.getLag())
                        .setMessage(peer.getMessage() == null ? "" : peer.getMessage())
                        .build()));
            }
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }

        @Override
        public void health(HealthRequest request, StreamObserver<HealthResponse> responseObserver) {
            responseObserver.onNext(HealthResponse.newBuilder().setHealthy(true).build());
            responseObserver.onCompleted();
        }

        @Override
        public void append(AppendRequest request, StreamObserver<AppendResponse> responseObserver) {
            responseObserver.onError(io.grpc.Status.UNIMPLEMENTED.withDescription("Use HTTP /append on the static leader in this alpha").asRuntimeException());
        }

        @Override
        public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
            responseObserver.onError(io.grpc.Status.UNIMPLEMENTED.withDescription("Use HTTP /read in this alpha").asRuntimeException());
        }
    }
}
