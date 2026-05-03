package com.projects.logstore.core;

import java.nio.file.Path;
import java.util.Objects;

public final class LogStoreConfig {
    private final Path dataDir;
    private final int partitions;
    private final Durability durability;

    private LogStoreConfig(Builder builder) {
        this.dataDir = Objects.requireNonNull(builder.dataDir, "dataDir");
        this.partitions = builder.partitions;
        this.durability = Objects.requireNonNull(builder.durability, "durability");
        if (partitions <= 0) {
            throw new IllegalArgumentException("partitions must be greater than zero");
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

    public static final class Builder {
        private Path dataDir = Path.of("data/logstore");
        private int partitions = 16;
        private Durability durability = Durability.FSYNC_EVERY_WRITE;

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

        public LogStoreConfig build() {
            return new LogStoreConfig(this);
        }
    }
}
