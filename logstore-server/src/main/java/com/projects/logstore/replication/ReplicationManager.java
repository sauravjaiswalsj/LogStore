package com.projects.logstore.replication;

import com.logstore.core.api.AppendResult;
import com.logstore.core.api.LogStore;
import com.projects.logstore.cluster.AckMode;
import com.projects.logstore.config.ClusterProperties;
import com.projects.logstore.dto.PeerStatusDTO;
import com.projects.logstore.dto.ReplicationRecordDTO;
import com.projects.logstore.dto.ReplicationResultDTO;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class ReplicationManager {
    private final ClusterProperties clusterProperties;
    private final ReplicationClient client;
    private final LogStore logStore;
    private volatile long commitOffset = -1L;

    public ReplicationManager(ClusterProperties clusterProperties, ReplicationClient client, LogStore logStore) {
        this.clusterProperties = clusterProperties;
        this.client = client;
        this.logStore = logStore;
    }

    public boolean isLeader() {
        return clusterProperties.isLeader();
    }

    public AckMode ackMode() {
        return clusterProperties.getAckMode();
    }

    public long commitOffset() {
        return commitOffset;
    }

    public ReplicationOutcome replicate(AppendResult result, String key, String value) {
        if (clusterProperties.getReplicationFactor() <= 1 || clusterProperties.getPeers().isEmpty()) {
            commitOffset = Math.max(commitOffset, result.offset());
            return new ReplicationOutcome(true, 1, List.of());
        }

        ReplicationRecordDTO record = new ReplicationRecordDTO();
        record.setStream(result.stream());
        record.setKey(key);
        record.setValue(value);
        record.setOffset(result.offset());
        record.setTimestamp(result.timestamp());

        List<PeerStatusDTO> statuses = new ArrayList<>();
        int persisted = 1;
        for (String peer : clusterProperties.getPeers()) {
            PeerStatusDTO status = new PeerStatusDTO();
            status.setPeer(peer);
            try {
                ReplicationResultDTO response = client.replicate(peer, record);
                if (response != null && response.isSuccess()) {
                    persisted++;
                    status.setHealthy(true);
                    status.setLatestOffset(record.getOffset());
                    status.setLag(0L);
                } else if (response != null && response.getExpectedOffset() >= 0 && response.getExpectedOffset() < record.getOffset()) {
                    int caughtUp = catchUp(peer, record.getStream(), response.getExpectedOffset(), record.getOffset());
                    ReplicationResultDTO retry = client.replicate(peer, record);
                    if (retry != null && retry.isSuccess()) {
                        persisted++;
                        status.setHealthy(true);
                        status.setLatestOffset(record.getOffset());
                        status.setLag(0L);
                        status.setMessage("caught up " + caughtUp + " records");
                    } else {
                        status.setMessage(retry == null ? "no response after catch-up" : retry.getMessage());
                    }
                } else {
                    status.setMessage(response == null ? "no response" : response.getMessage());
                }
            } catch (RuntimeException ex) {
                status.setMessage(ex.getMessage());
            }
            if (!status.isHealthy()) {
                status.setLag(Math.max(0L, record.getOffset() - status.getLatestOffset()));
            }
            statuses.add(status);
        }

        int required = clusterProperties.getAckMode() == AckMode.LEADER_ONLY
                ? 1
                : (clusterProperties.getReplicationFactor() / 2) + 1;
        boolean success = persisted >= required;
        if (success) {
            commitOffset = Math.max(commitOffset, result.offset());
        }
        return new ReplicationOutcome(success, persisted, statuses);
    }

    public ReplicationResultDTO appendFromLeader(ReplicationRecordDTO record) {
        ReplicationResultDTO result = new ReplicationResultDTO();
        try {
            logStore.appendReplicated(
                    record.getStream(),
                    record.getKey(),
                    record.getValue().getBytes(StandardCharsets.UTF_8),
                    record.getOffset(),
                    record.getTimestamp());
            commitOffset = Math.max(commitOffset, record.getOffset());
            result.setSuccess(true);
            result.setOffset(record.getOffset());
            result.setExpectedOffset(record.getOffset() + 1L);
        } catch (RuntimeException ex) {
            long expected = logStore.tablets().stream()
                    .filter(tablet -> tablet.tabletId() == logStore.tabletForStreamId(record.getStream()))
                    .findFirst()
                    .map(com.logstore.core.api.TabletInfo::nextOffset)
                    .orElse(-1L);
            result.setSuccess(false);
            result.setOffset(record.getOffset());
            result.setExpectedOffset(expected);
            result.setMessage(ex.getMessage());
        }
        return result;
    }

    public List<PeerStatusDTO> peerStatuses() {
        List<PeerStatusDTO> statuses = new ArrayList<>();
        long leaderLatest = latestOffset();
        for (String peer : clusterProperties.getPeers()) {
            PeerStatusDTO status = new PeerStatusDTO();
            status.setPeer(peer);
            try {
                var peerStatus = client.status(peer);
                status.setHealthy(peerStatus != null);
                status.setLatestOffset(peerStatus == null ? -1L : peerStatus.getLatestOffset());
                status.setLag(Math.max(0L, leaderLatest - status.getLatestOffset()));
            } catch (RuntimeException ex) {
                status.setMessage(ex.getMessage());
                status.setLag(Math.max(0L, leaderLatest + 1L));
            }
            statuses.add(status);
        }
        return statuses;
    }

    public long latestOffset() {
        return logStore.tablets().stream()
                .mapToLong(com.logstore.core.api.TabletInfo::latestOffset)
                .max()
                .orElse(-1L);
    }

    private int catchUp(String peer, String stream, long expectedOffset, long throughOffset) {
        int sent = 0;
        int tabletId = logStore.tabletForStreamId(stream);
        for (com.logstore.core.api.LogRecord record : logStore.readTabletForAdmin(tabletId, expectedOffset, 1000)) {
            if (record.offset() >= throughOffset) {
                break;
            }
            ReplicationRecordDTO dto = new ReplicationRecordDTO();
            dto.setStream(record.stream());
            dto.setKey(record.key());
            dto.setValue(new String(record.value(), StandardCharsets.UTF_8));
            dto.setOffset(record.offset());
            dto.setTimestamp(record.timestamp());
            client.replicate(peer, dto);
            sent++;
        }
        return sent;
    }

    public record ReplicationOutcome(boolean success, int persistedReplicas, List<PeerStatusDTO> peers) {
    }
}
