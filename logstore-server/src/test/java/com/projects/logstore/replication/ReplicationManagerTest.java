package com.projects.logstore.replication;

import com.logstore.core.api.AppendResult;
import com.logstore.core.api.Durability;
import com.logstore.core.api.LogStore;
import com.logstore.core.api.LogStoreConfig;
import com.projects.logstore.cluster.AckMode;
import com.projects.logstore.config.ClusterProperties;
import com.projects.logstore.dto.ClusterStatusDTO;
import com.projects.logstore.dto.FetchRecordsDTO;
import com.projects.logstore.dto.ReplicationRecordDTO;
import com.projects.logstore.dto.ReplicationResultDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReplicationManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void quorumSucceedsWithOneFollowerAck() {
        try (LogStore leaderStore = openStore(tempDir.resolve("leader"))) {
            AppendResult append = leaderStore.append("orders", "ORD-1", bytes("one"));
            FakePeerClient client = new FakePeerClient();
            client.successfulPeers.add("node-2");

            ReplicationManager manager = new ReplicationManager(leaderProperties(), client, leaderStore);
            ReplicationManager.ReplicationOutcome outcome = manager.replicate(append, "ORD-1", "one");

            assertThat(outcome.success()).isTrue();
            assertThat(outcome.persistedReplicas()).isEqualTo(2);
            assertThat(manager.commitOffset()).isEqualTo(0L);
        }
    }

    @Test
    void catchUpSendsMissingRecordsBeforeRetryingCurrentRecord() {
        try (LogStore leaderStore = openStore(tempDir.resolve("leader"))) {
            leaderStore.append("orders", "ORD-1", bytes("one"));
            leaderStore.append("orders", "ORD-2", bytes("two"));
            AppendResult append = leaderStore.append("orders", "ORD-3", bytes("three"));
            FakePeerClient client = new FakePeerClient();
            client.expectedOffset = 1L;
            ClusterProperties properties = leaderProperties();
            properties.setPeers(List.of("node-2"));

            ReplicationManager manager = new ReplicationManager(properties, client, leaderStore);
            ReplicationManager.ReplicationOutcome outcome = manager.replicate(append, "ORD-3", "three");

            assertThat(outcome.success()).isTrue();
            assertThat(client.replicatedOffsets).containsExactly(2L, 1L, 2L);
        }
    }

    @Test
    void followerPullsMissingRecordsFromLeaderFetchEndpoint() {
        try (LogStore followerStore = openStore(tempDir.resolve("follower"))) {
            FakePeerClient client = new FakePeerClient();
            client.fetchRecords.add(replicationRecord("orders", "ORD-1", "one", 0L));
            client.fetchRecords.add(replicationRecord("orders", "ORD-2", "two", 1L));
            ClusterProperties properties = followerProperties();

            ReplicationManager manager = new ReplicationManager(properties, client, followerStore);
            ReplicationManager.CatchUpResult result = manager.catchUpFromLeader();

            assertThat(result.success()).isTrue();
            assertThat(result.appliedRecords()).isEqualTo(2);
            assertThat(followerStore.read("orders", 0, 10)).extracting(com.logstore.core.api.LogRecord::key)
                    .containsExactly("ORD-1", "ORD-2");
        }
    }

    @Test
    void followerRejectsOutOfOrderReplicatedAppend() {
        try (LogStore followerStore = openStore(tempDir.resolve("follower"))) {
            ReplicationManager manager = new ReplicationManager(followerProperties(), new FakePeerClient(), followerStore);

            ReplicationResultDTO result = manager.appendFromLeader(replicationRecord("orders", "ORD-2", "two", 1L));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getExpectedOffset()).isZero();
        }
    }

    private static ClusterProperties leaderProperties() {
        ClusterProperties properties = new ClusterProperties();
        properties.setLeader(true);
        properties.setReplicationFactor(3);
        properties.setAckMode(AckMode.QUORUM);
        properties.setPeers(List.of("node-2", "node-3"));
        return properties;
    }

    private static ClusterProperties followerProperties() {
        ClusterProperties properties = new ClusterProperties();
        properties.setLeader(false);
        properties.setLeaderUrl("leader");
        properties.setReplicationFactor(3);
        properties.setAckMode(AckMode.QUORUM);
        properties.setPeers(List.of("leader"));
        properties.setCatchUpBatchSize(1000);
        return properties;
    }

    private static LogStore openStore(Path dataDir) {
        return LogStore.open(LogStoreConfig.builder()
                .dataDir(dataDir)
                .partitions(1)
                .durability(Durability.ASYNC_FLUSH)
                .build());
    }

    private static ReplicationRecordDTO replicationRecord(String stream, String key, String value, long offset) {
        ReplicationRecordDTO record = new ReplicationRecordDTO();
        record.setStream(stream);
        record.setKey(key);
        record.setValue(value);
        record.setOffset(offset);
        record.setTimestamp(1734150400000L + offset);
        return record;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static final class FakePeerClient implements ReplicationPeerClient {
        private final List<String> successfulPeers = new ArrayList<>();
        private final List<Long> replicatedOffsets = new ArrayList<>();
        private final List<ReplicationRecordDTO> fetchRecords = new ArrayList<>();
        private long expectedOffset = -1L;

        @Override
        public ReplicationResultDTO replicate(String peer, ReplicationRecordDTO record) {
            replicatedOffsets.add(record.getOffset());
            ReplicationResultDTO response = new ReplicationResultDTO();
            response.setOffset(record.getOffset());
            if (expectedOffset >= 0 && record.getOffset() > expectedOffset) {
                response.setSuccess(false);
                response.setExpectedOffset(expectedOffset);
                response.setMessage("behind");
                return response;
            }
            boolean success = successfulPeers.isEmpty() ? expectedOffset >= 0 : successfulPeers.contains(peer);
            if (success) {
                expectedOffset = record.getOffset() + 1L;
            }
            response.setSuccess(success);
            response.setExpectedOffset(expectedOffset);
            return response;
        }

        @Override
        public FetchRecordsDTO fetchFromOffset(String peer, int tabletId, long offset, int limit) {
            FetchRecordsDTO dto = new FetchRecordsDTO();
            dto.setTabletId(tabletId);
            dto.setOffset(offset);
            dto.setLimit(limit);
            dto.setRecords(fetchRecords.stream()
                    .filter(record -> record.getOffset() >= offset)
                    .limit(limit)
                    .toList());
            dto.setNextOffset(dto.getRecords().isEmpty() ? offset : dto.getRecords().get(dto.getRecords().size() - 1).getOffset() + 1L);
            return dto;
        }

        @Override
        public ClusterStatusDTO status(String peer) {
            ClusterStatusDTO status = new ClusterStatusDTO();
            status.setLatestOffset(expectedOffset - 1L);
            return status;
        }
    }
}
