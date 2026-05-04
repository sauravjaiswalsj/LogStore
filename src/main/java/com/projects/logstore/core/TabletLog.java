package com.projects.logstore.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

final class TabletLog {
    private final int tabletId;
    private final Path path;
    private final Durability durability;
    private final Clock clock;
    private long nextOffset;

    TabletLog(int tabletId, Path dataDir, Durability durability, Clock clock) throws IOException {
        this.tabletId = tabletId;
        this.path = dataDir.resolve("tablet-" + tabletId + ".log");
        this.durability = durability;
        this.clock = clock;
        Files.createDirectories(dataDir);
        this.nextOffset = recoverNextOffset();
    }

    synchronized AppendResult append(String stream, String key, byte[] value) throws IOException {
        long offset = nextOffset;
        long timestamp = clock.millis();
        byte[] encoded = RecordCodec.encode(offset, timestamp, stream, key, value);
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            writeFully(channel, ByteBuffer.wrap(encoded));
            if (durability == Durability.FSYNC_EVERY_WRITE) {
                channel.force(true);
            }
        }
        nextOffset++;
        return new AppendResult(stream, tabletId, offset, timestamp);
    }

    synchronized List<LogRecord> read(String stream, long offset, int limit) throws IOException {
        if (limit <= 0 || !Files.exists(path)) {
            return List.of();
        }

        List<LogRecord> records = new ArrayList<>();
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            while (records.size() < limit) {
                RecordCodec.DecodedRecord record = readNext(channel);
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
        return Files.exists(path) ? Files.size(path) : 0L;
    }

    private long recoverNextOffset() throws IOException {
        if (!Files.exists(path)) {
            return 0L;
        }

        long validBytes = 0L;
        long expectedOffset = 0L;
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            while (true) {
                long recordStart = channel.position();
                RecordCodec.DecodedRecord record = readNext(channel);
                if (record == null || record.offset() != expectedOffset) {
                    break;
                }
                validBytes = channel.position();
                expectedOffset = record.offset() + 1L;
                if (validBytes == recordStart) {
                    break;
                }
            }
        }

        if (validBytes == 0L && Files.size(path) > 0L) {
            long migratedNextOffset = migrateLegacyTextLog();
            if (migratedNextOffset >= 0L) {
                return migratedNextOffset;
            }
        }

        if (validBytes > 0L || Files.size(path) == 0L) {
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
                channel.truncate(validBytes);
            }
        }
        return expectedOffset;
    }

    private long migrateLegacyTextLog() throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<byte[]> framedRecords = new ArrayList<>();
        long expectedOffset = 0L;

        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|", 4);
            if (parts.length != 4) {
                return -1L;
            }
            long offset;
            long timestamp;
            try {
                offset = Long.parseLong(parts[0]);
                timestamp = Long.parseLong(parts[1]);
            } catch (NumberFormatException ex) {
                return -1L;
            }
            if (offset != expectedOffset) {
                return -1L;
            }
            framedRecords.add(RecordCodec.encode(offset, timestamp, "default", parts[2], parts[3].getBytes(StandardCharsets.UTF_8)));
            expectedOffset++;
        }

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (byte[] framedRecord : framedRecords) {
                writeFully(channel, ByteBuffer.wrap(framedRecord));
            }
            if (durability == Durability.FSYNC_EVERY_WRITE) {
                channel.force(true);
            }
        }
        return expectedOffset;
    }

    private static RecordCodec.DecodedRecord readNext(FileChannel channel) throws IOException {
        ByteBuffer prefix = ByteBuffer.allocate(RecordCodec.PREFIX_BYTES);
        if (!readFullyOrEof(channel, prefix)) {
            return null;
        }
        prefix.flip();
        int magic = prefix.getInt();
        short version = prefix.getShort();
        int bodyLength = prefix.getInt();
        if (magic != RecordCodec.MAGIC || version != RecordCodec.VERSION || bodyLength < 0) {
            return null;
        }

        ByteBuffer record = ByteBuffer.allocate(RecordCodec.PREFIX_BYTES + bodyLength);
        record.putInt(magic);
        record.putShort(version);
        record.putInt(bodyLength);
        ByteBuffer body = ByteBuffer.allocate(bodyLength);
        if (!readFullyOrEof(channel, body)) {
            return null;
        }
        body.flip();
        record.put(body);
        record.flip();
        return RecordCodec.decode(record).orElse(null);
    }

    private static boolean readFullyOrEof(FileChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer);
            if (read == -1) {
                if (buffer.position() == 0) {
                    return false;
                }
                return false;
            }
        }
        return true;
    }

    private static void writeFully(FileChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }
}
