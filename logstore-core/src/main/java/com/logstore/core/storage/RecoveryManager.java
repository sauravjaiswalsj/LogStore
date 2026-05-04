package com.logstore.core.storage;

import com.logstore.core.api.Durability;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

final class RecoveryManager {
    private RecoveryManager() {
    }

    static long recoverNextOffset(Path path, Durability durability) throws IOException {
        if (!Files.exists(path)) {
            return 0L;
        }

        long validBytes = 0L;
        long expectedOffset = 0L;
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            while (true) {
                long recordStart = channel.position();
                RecordDecoder.DecodedRecord record = readNext(channel);
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
            long migratedNextOffset = migrateLegacyTextLog(path, durability);
            if (migratedNextOffset >= 0L) {
                return migratedNextOffset;
            }
        }

        if (validBytes > 0L || Files.size(path) == 0L || startsWithLogStoreMagic(path)) {
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
                channel.truncate(validBytes);
            }
        }
        return expectedOffset;
    }

    static RecordDecoder.DecodedRecord readNext(FileChannel channel) throws IOException {
        ByteBuffer prefix = ByteBuffer.allocate(RecordEncoder.PREFIX_BYTES);
        if (!readFullyOrEof(channel, prefix)) {
            return null;
        }
        prefix.flip();
        int magic = prefix.getInt();
        short version = prefix.getShort();
        int bodyLength = prefix.getInt();
        if (magic != RecordEncoder.MAGIC
                || version != RecordEncoder.VERSION
                || bodyLength < RecordEncoder.BODY_FIXED_BYTES
                || bodyLength > RecordEncoder.MAX_RECORD_BODY_BYTES) {
            return null;
        }

        ByteBuffer record = ByteBuffer.allocate(RecordEncoder.PREFIX_BYTES + bodyLength);
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
        return RecordDecoder.decode(record).orElse(null);
    }

    private static long migrateLegacyTextLog(Path path, Durability durability) throws IOException {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return -1L;
        }
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
            framedRecords.add(RecordEncoder.encode(offset, timestamp, "default", parts[2], parts[3].getBytes(StandardCharsets.UTF_8)));
            expectedOffset++;
        }

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (byte[] framedRecord : framedRecords) {
                Tablet.writeFully(channel, ByteBuffer.wrap(framedRecord));
            }
            if (durability == Durability.FSYNC_EVERY_WRITE) {
                channel.force(true);
            }
        }
        return expectedOffset;
    }

    private static boolean readFullyOrEof(FileChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer);
            if (read == -1) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWithLogStoreMagic(Path path) throws IOException {
        if (Files.size(path) < Integer.BYTES) {
            return false;
        }
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer magic = ByteBuffer.allocate(Integer.BYTES);
            if (!readFullyOrEof(channel, magic)) {
                return false;
            }
            magic.flip();
            return magic.getInt() == RecordEncoder.MAGIC;
        }
    }
}
