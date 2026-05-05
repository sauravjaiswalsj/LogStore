package com.projects.logstore.replication;

import com.projects.logstore.dto.ClusterStatusDTO;
import com.projects.logstore.dto.FetchRecordsDTO;
import com.projects.logstore.dto.PeerStatusDTO;
import com.projects.logstore.dto.ReplicationRecordDTO;
import com.projects.logstore.dto.ReplicationResultDTO;
import com.projects.logstore.proto.FetchFromOffsetRequest;
import com.projects.logstore.proto.LogStoreServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ReplicationClient implements ReplicationPeerClient {
    private final ConcurrentMap<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    @Override
    public ReplicationResultDTO replicate(String peer, ReplicationRecordDTO record) {
        var response = stub(peer).replicate(toProto(record));
        ReplicationResultDTO dto = new ReplicationResultDTO();
        dto.setSuccess(response.getSuccess());
        dto.setOffset(response.getOffset());
        dto.setExpectedOffset(response.getExpectedOffset());
        dto.setMessage(response.getMessage());
        return dto;
    }

    @Override
    public FetchRecordsDTO fetchFromOffset(String peer, int tabletId, long offset, int limit) {
        var response = stub(peer).fetchFromOffset(FetchFromOffsetRequest.newBuilder()
                .setTabletId(tabletId)
                .setOffset(offset)
                .setLimit(limit)
                .build());
        FetchRecordsDTO dto = new FetchRecordsDTO();
        dto.setTabletId(tabletId);
        dto.setOffset(offset);
        dto.setLimit(limit);
        dto.setNextOffset(response.getNextOffset());
        dto.setRecords(response.getRecordsList().stream()
                .map(record -> {
                    ReplicationRecordDTO mapped = new ReplicationRecordDTO();
                    mapped.setStream(record.getStream());
                    mapped.setKey(record.getKey());
                    mapped.setValue(record.getValue().toString(StandardCharsets.UTF_8));
                    mapped.setOffset(record.getOffset());
                    mapped.setTimestamp(record.getTimestamp());
                    return mapped;
                })
                .toList());
        return dto;
    }

    @Override
    public ClusterStatusDTO status(String peer) {
        var response = stub(peer).clusterStatus(com.projects.logstore.proto.ClusterStatusRequest.newBuilder().build());
        ClusterStatusDTO dto = new ClusterStatusDTO();
        dto.setNodeId(response.getNodeId());
        dto.setLeader(response.getLeader());
        dto.setAckMode(response.getAckMode());
        dto.setLatestOffset(response.getLatestOffset());
        dto.setCommitOffset(response.getCommitOffset());
        dto.setPeers(response.getPeersList().stream()
                .map(peerStatus -> {
                    PeerStatusDTO mapped = new PeerStatusDTO();
                    mapped.setPeer(peerStatus.getPeer());
                    mapped.setHealthy(peerStatus.getHealthy());
                    mapped.setLatestOffset(peerStatus.getLatestOffset());
                    mapped.setLag(peerStatus.getLag());
                    mapped.setMessage(peerStatus.getMessage());
                    return mapped;
                })
                .toList());
        return dto;
    }

    private LogStoreServiceGrpc.LogStoreServiceBlockingStub stub(String peer) {
        return LogStoreServiceGrpc.newBlockingStub(channels.computeIfAbsent(normalizeTarget(peer), target ->
                ManagedChannelBuilder.forTarget(target).usePlaintext().build()));
    }

    private static com.projects.logstore.proto.ReplicationRecord toProto(ReplicationRecordDTO record) {
        return com.projects.logstore.proto.ReplicationRecord.newBuilder()
                .setStream(record.getStream())
                .setKey(record.getKey())
                .setValue(com.google.protobuf.ByteString.copyFrom(record.getValue().getBytes(StandardCharsets.UTF_8)))
                .setOffset(record.getOffset())
                .setTimestamp(record.getTimestamp())
                .build();
    }

    private static String normalizeTarget(String peer) {
        return peer.replace("http://", "").replace("https://", "");
    }
}
