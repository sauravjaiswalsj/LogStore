package com.logstore.core.api;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ClusterConfig {
    private final String nodeId;
    private final Path dataDir;
    private final List<String> peers;
    private final boolean leader;
    private final int replicationFactor;
    private final AckMode ackMode;
    private final LogStoreConfig logStoreConfig;

    private ClusterConfig(Builder builder) {
        this.nodeId = Objects.requireNonNull(builder.nodeId, "nodeId");
        this.dataDir = Objects.requireNonNull(builder.dataDir, "dataDir");
        this.peers = List.copyOf(builder.peers);
        this.leader = builder.leader;
        this.replicationFactor = builder.replicationFactor;
        this.ackMode = Objects.requireNonNull(builder.ackMode, "ackMode");
        if (nodeId.isBlank() || replicationFactor <= 0) {
            throw new IllegalArgumentException("nodeId must be present and replicationFactor must be positive");
        }
        this.logStoreConfig = LogStoreConfig.builder()
                .dataDir(dataDir)
                .durability(builder.durability)
                .partitions(builder.partitions)
                .batchSize(builder.batchSize)
                .flushIntervalMillis(builder.flushIntervalMillis)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String nodeId() { return nodeId; }
    public Path dataDir() { return dataDir; }
    public List<String> peers() { return peers; }
    public boolean leader() { return leader; }
    public int replicationFactor() { return replicationFactor; }
    public AckMode ackMode() { return ackMode; }
    LogStoreConfig logStoreConfig() { return logStoreConfig; }

    public static final class Builder {
        private String nodeId = "node-1";
        private Path dataDir = Path.of("data/logstore");
        private List<String> peers = new ArrayList<>();
        private boolean leader = true;
        private int replicationFactor = 1;
        private AckMode ackMode = AckMode.LEADER_ONLY;
        private Durability durability = Durability.BATCHED_FSYNC;
        private int partitions = 16;
        private int batchSize = 128;
        private long flushIntervalMillis = 5L;

        public Builder nodeId(String nodeId) { this.nodeId = nodeId; return this; }
        public Builder dataDir(Path dataDir) { this.dataDir = dataDir; return this; }
        public Builder peers(List<String> peers) { this.peers = new ArrayList<>(peers); return this; }
        public Builder leader(boolean leader) { this.leader = leader; return this; }
        public Builder replicationFactor(int replicationFactor) { this.replicationFactor = replicationFactor; return this; }
        public Builder ackMode(AckMode ackMode) { this.ackMode = ackMode; return this; }
        public Builder durability(Durability durability) { this.durability = durability; return this; }
        public Builder partitions(int partitions) { this.partitions = partitions; return this; }
        public Builder batchSize(int batchSize) { this.batchSize = batchSize; return this; }
        public Builder flushIntervalMillis(long flushIntervalMillis) { this.flushIntervalMillis = flushIntervalMillis; return this; }
        public ClusterConfig build() { return new ClusterConfig(this); }
    }
}
