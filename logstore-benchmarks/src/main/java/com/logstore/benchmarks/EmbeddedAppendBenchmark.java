package com.logstore.benchmarks;

import com.logstore.core.api.Durability;
import com.logstore.core.api.LogStore;
import com.logstore.core.api.LogStoreConfig;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@State(Scope.Benchmark)
public class EmbeddedAppendBenchmark {
    @Param({"256", "1024", "2048", "10240"})
    public int payloadBytes;

    @Param({"BATCHED_FSYNC", "ASYNC_FLUSH", "FSYNC_EVERY_WRITE"})
    public String durability;

    private LogStore store;
    private byte[] payload;
    private AtomicLong sequence;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        Path dataDir = Files.createTempDirectory("logstore-jmh-");
        store = LogStore.open(LogStoreConfig.builder()
                .dataDir(dataDir)
                .partitions(16)
                .durability(Durability.valueOf(durability))
                .batchSize(256)
                .flushIntervalMillis(5)
                .build());
        payload = new byte[payloadBytes];
        ThreadLocalRandom.current().nextBytes(payload);
        sequence = new AtomicLong();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        store.close();
    }

    @Benchmark
    public long append() {
        long id = sequence.incrementAndGet();
        return store.append("orders-" + (id % 16), "ORD-" + id, payload).offset();
    }
}
