package com.logstore.core.storage;

import com.logstore.core.api.AppendResult;
import com.logstore.core.api.Durability;
import com.logstore.core.api.LogRecord;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

public final class Tablet {
    private final int tabletId;
    private final Segment segment;
    private final Durability durability;
    private final Clock clock;
    private long nextOffset;

    Tablet(int tabletId, Path dataDir, Durability durability, Clock clock) throws IOException {
        this.tabletId = tabletId;
        this.segment = new Segment(dataDir, tabletId);
        this.durability = durability;
        this.clock = clock;
        Files.createDirectories(dataDir);
        this.nextOffset = RecoveryManager.recoverNextOffset(segment.path(), durability);
    }

    public synchronized AppendResult append(String stream, String key, byte[] value) throws IOException {
        long offset = nextOffset;
        long timestamp = clock.millis();
        byte[] encoded = RecordEncoder.encode(offset, timestamp, stream, key, value);
        try {
            try (FileChannel channel = FileChannel.open(segment.path(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                writeFully(channel, ByteBuffer.wrap(encoded));
                if (durability == Durability.FSYNC_EVERY_WRITE) {
                    channel.force(true);
                }
            }
            nextOffset++;
        } catch (IOException ex) {
            try {
                nextOffset = RecoveryManager.recoverNextOffset(segment.path(), durability);
            } catch (IOException recoveryEx) {
                ex.addSuppressed(recoveryEx);
            }
            throw ex;
        }
        return new AppendResult(stream, tabletId, offset, timestamp);
    }

    public synchronized List<LogRecord> read(String stream, long offset, int limit) throws IOException {
        if (limit <= 0 || !Files.exists(segment.path())) {
            return List.of();
        }

        List<LogRecord> records = new ArrayList<>();
        try (FileChannel channel = FileChannel.open(segment.path(), StandardOpenOption.READ)) {
            while (records.size() < limit) {
                RecordDecoder.DecodedRecord record = RecoveryManager.readNext(channel);
                if (record == null) {
                    break;
                }
                if (record.offset() >= offset && stream.equals(record.stream())) {
                    records.add(new LogRecord(record.stream(), record.offset(), record.timestamp(), record.key(), record.value()));
                }
            }
        }
        return records;
    }

    public synchronized List<LogRecord> readAll(long offset, int limit) throws IOException {
        if (limit <= 0 || !Files.exists(segment.path())) {
            return List.of();
        }

        List<LogRecord> records = new ArrayList<>();
        try (FileChannel channel = FileChannel.open(segment.path(), StandardOpenOption.READ)) {
            while (records.size() < limit) {
                RecordDecoder.DecodedRecord record = RecoveryManager.readNext(channel);
                if (record == null) {
                    break;
                }
                if (record.offset() >= offset) {
                    records.add(new LogRecord(record.stream(), record.offset(), record.timestamp(), record.key(), record.value()));
                }
            }
        }
        return records;
    }

    int tabletId() {
        return tabletId;
    }

    synchronized long nextOffset() {
        return nextOffset;
    }

    synchronized long latestOffset() {
        return nextOffset == 0 ? -1L : nextOffset - 1L;
    }

    long sizeBytes() throws IOException {
        return Files.exists(segment.path()) ? Files.size(segment.path()) : 0L;
    }

    static void writeFully(FileChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }
}
