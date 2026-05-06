package com.logstore.core.api;

import java.io.Closeable;
import java.util.List;

public final class LogStoreCluster implements Closeable {
    private final ClusterConfig config;
    private final LogStore localStore;

    private LogStoreCluster(ClusterConfig config) {
        this.config = config;
        this.localStore = LogStore.open(config.logStoreConfig());
    }

    public static LogStoreCluster open(ClusterConfig config) {
        return new LogStoreCluster(config);
    }

    public AppendResult append(String stream, String key, byte[] value) {
        if (!config.leader()) {
            throw new IllegalStateException("static V0.3 alpha accepts client appends only on the configured leader");
        }
        return localStore.append(stream, key, value);
    }

    public List<LogRecord> read(String stream, long offset, int limit) {
        return localStore.read(stream, offset, limit);
    }

    public ClusterConfig config() {
        return config;
    }

    @Override
    public void close() {
        localStore.close();
    }
}
