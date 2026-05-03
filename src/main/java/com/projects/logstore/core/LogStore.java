package com.projects.logstore.core;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LogStore implements Closeable {
    private final LogStoreConfig config;
    private final TabletLog[] tablets;

    private LogStore(LogStoreConfig config, Clock clock) throws IOException {
        this.config = config;
        this.tablets = new TabletLog[config.partitions()];
        for (int i = 0; i < tablets.length; i++) {
            tablets[i] = new TabletLog(i, config.dataDir(), config.durability(), clock);
        }
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
        validate(stream, key, value);
        try {
            return tabletForStream(stream).append(stream, key, value);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to append record", ex);
        }
    }

    public List<LogRecord> read(String stream, long offset, int limit) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset cannot be negative");
        }
        if (limit <= 0) {
            return List.of();
        }
        try {
            return tabletForStream(stream).read(stream, offset, limit);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read records", ex);
        }
    }

    public List<LogRecord> readTablet(int tabletId, long offset, int limit) {
        if (tabletId < 0 || tabletId >= tablets.length) {
            throw new IllegalArgumentException("Unknown tabletId " + tabletId);
        }
        try {
            return tablets[tabletId].read("tablet-" + tabletId, offset, limit);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read tablet records", ex);
        }
    }

    public int tabletForStreamId(String stream) {
        return route(stream);
    }

    public int partitionCount() {
        return tablets.length;
    }

    public List<TabletInfo> tablets() {
        List<TabletInfo> infos = new ArrayList<>(tablets.length);
        for (TabletLog tablet : tablets) {
            try {
                infos.add(new TabletInfo(tablet.tabletId(), tablet.nextOffset(), tablet.latestOffset(), tablet.sizeBytes()));
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to inspect tablet " + tablet.tabletId(), ex);
            }
        }
        return infos;
    }

    @Override
    public void close() {
    }

    private TabletLog tabletForStream(String stream) {
        return tablets[route(stream)];
    }

    private int route(String stream) {
        return Math.floorMod(Objects.requireNonNull(stream, "stream").hashCode(), config.partitions());
    }

    private static void validate(String stream, String key, byte[] value) {
        if (stream == null || stream.isBlank()) {
            throw new IllegalArgumentException("stream cannot be null or blank");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
        Objects.requireNonNull(value, "value");
    }

    public record TabletInfo(int tabletId, long nextOffset, long latestOffset, long sizeBytes) {
    }
}
