package com.logstore.core.api;

import com.logstore.core.storage.PartitionManager;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class LogStore implements Closeable {
    private final PartitionManager partitionManager;
    private boolean closed;

    private LogStore(LogStoreConfig config, Clock clock) throws IOException {
        this.partitionManager = new PartitionManager(config, clock);
    }

    public static LogStore open(String dataDir) {
        return open(LogStoreConfig.builder().dataDir(Path.of(dataDir)).build());
    }

    public static LogStore open(Path dataDir) {
        return open(LogStoreConfig.builder().dataDir(dataDir).build());
    }

    public static LogStore open(LogStoreConfig config) {
        try {
            return new LogStore(config, Clock.systemUTC());
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to open LogStore", ex);
        }
    }

    public AppendResult append(String stream, String key, byte[] value) {
        ensureOpen();
        validateAppend(stream, key, value);
        try {
            return partitionManager.tabletForStream(stream).append(stream, key, value);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to append record", ex);
        }
    }

    public List<LogRecord> read(String stream, long offset, int limit) {
        ensureOpen();
        validateRead(stream, offset);
        if (limit <= 0) {
            return List.of();
        }
        try {
            return partitionManager.tabletForStream(stream).read(stream, offset, limit);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read records", ex);
        }
    }

    public List<LogRecord> readTabletForAdmin(int tabletId, long offset, int limit) {
        ensureOpen();
        if (offset < 0) {
            throw new IllegalArgumentException("offset cannot be negative");
        }
        if (limit <= 0) {
            return List.of();
        }
        try {
            return partitionManager.tabletById(tabletId).readAll(offset, limit);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read tablet records", ex);
        }
    }

    public void replay(String stream, long offset, int batchSize, RecordHandler handler) {
        ensureOpen();
        Objects.requireNonNull(handler, "handler");
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }

        long nextOffset = offset;
        while (true) {
            List<LogRecord> records = read(stream, nextOffset, batchSize);
            if (records.isEmpty()) {
                return;
            }
            for (LogRecord record : records) {
                handler.onRecord(record);
                nextOffset = record.offset() + 1L;
            }
            if (records.size() < batchSize) {
                return;
            }
        }
    }

    public int tabletForStreamId(String stream) {
        ensureOpen();
        validateStream(stream);
        return partitionManager.tabletIdForStream(stream);
    }

    public int partitionCount() {
        ensureOpen();
        return partitionManager.partitionCount();
    }

    public List<TabletInfo> tablets() {
        ensureOpen();
        try {
            return partitionManager.tablets();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to inspect tablets", ex);
        }
    }

    @Override
    public void close() {
        closed = true;
    }

    private static void validateAppend(String stream, String key, byte[] value) {
        validateStream(stream);
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
        Objects.requireNonNull(value, "value");
    }

    private static void validateRead(String stream, long offset) {
        validateStream(stream);
        if (offset < 0) {
            throw new IllegalArgumentException("offset cannot be negative");
        }
    }

    private static void validateStream(String stream) {
        if (stream == null || stream.isBlank()) {
            throw new IllegalArgumentException("stream cannot be null or blank");
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("LogStore is closed");
        }
    }
}
