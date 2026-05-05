package com.logstore.core.api;

import java.nio.file.Path;
import java.util.Objects;

public final class LogStoreConfig {
    private final Path dataDir;
    private final int partitions;
    private final Durability durability;
    private final int batchSize;
    private final long flushIntervalMillis;
    private final int indexInterval;
    private final long maxSegmentBytes;
    private final int queueCapacity;
    private final BackpressurePolicy backpressurePolicy;

    private LogStoreConfig(Builder builder) {
        this.dataDir = Objects.requireNonNull(builder.dataDir, "dataDir");
        this.partitions = builder.partitions;
        this.durability = Objects.requireNonNull(builder.durability, "durability");
        this.batchSize = builder.batchSize;
        this.flushIntervalMillis = builder.flushIntervalMillis;
        this.indexInterval = builder.indexInterval;
        this.maxSegmentBytes = builder.maxSegmentBytes;
        this.queueCapacity = builder.queueCapacity;
        this.backpressurePolicy = Objects.requireNonNull(builder.backpressurePolicy, "backpressurePolicy");
        if (partitions <= 0) {
            throw new IllegalArgumentException("partitions must be greater than zero");
        }
        if (batchSize <= 0 || flushIntervalMillis < 0 || indexInterval <= 0 || maxSegmentBytes <= 0 || queueCapacity <= 0) {
            throw new IllegalArgumentException("batchSize, indexInterval, maxSegmentBytes, and queueCapacity must be positive; flushIntervalMillis cannot be negative");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Path dataDir() {
        return dataDir;
    }

    public int partitions() {
        return partitions;
    }

    public Durability durability() {
        return durability;
    }

    public int batchSize() {
        return batchSize;
    }

    public long flushIntervalMillis() {
        return flushIntervalMillis;
    }

    public int indexInterval() {
        return indexInterval;
    }

    public long maxSegmentBytes() {
        return maxSegmentBytes;
    }

    public int queueCapacity() {
        return queueCapacity;
    }

    public BackpressurePolicy backpressurePolicy() {
        return backpressurePolicy;
    }

    public static final class Builder {
        private Path dataDir = Path.of("data/logstore");
        private int partitions = 16;
        private Durability durability = Durability.FSYNC_EVERY_WRITE;
        private int batchSize = 128;
        private long flushIntervalMillis = 5L;
        private int indexInterval = 128;
        private long maxSegmentBytes = Long.MAX_VALUE;
        private int queueCapacity = 8192;
        private BackpressurePolicy backpressurePolicy = BackpressurePolicy.BLOCK;

        public Builder dataDir(Path dataDir) {
            this.dataDir = dataDir;
            return this;
        }

        public Builder partitions(int partitions) {
            this.partitions = partitions;
            return this;
        }

        public Builder durability(Durability durability) {
            this.durability = durability;
            return this;
        }

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder flushIntervalMillis(long flushIntervalMillis) {
            this.flushIntervalMillis = flushIntervalMillis;
            return this;
        }

        public Builder indexInterval(int indexInterval) {
            this.indexInterval = indexInterval;
            return this;
        }

        public Builder maxSegmentBytes(long maxSegmentBytes) {
            this.maxSegmentBytes = maxSegmentBytes;
            return this;
        }

        public Builder queueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public Builder backpressurePolicy(BackpressurePolicy backpressurePolicy) {
            this.backpressurePolicy = backpressurePolicy;
            return this;
        }

        public LogStoreConfig build() {
            return new LogStoreConfig(this);
        }
    }
}
