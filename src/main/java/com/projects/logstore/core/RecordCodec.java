package com.projects.logstore.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.CRC32;

final class RecordCodec {
    static final int MAGIC = 0x4c535431;
    static final short VERSION = 1;
    static final int PREFIX_BYTES = Integer.BYTES + Short.BYTES + Integer.BYTES;
    private static final int BODY_FIXED_BYTES = Long.BYTES + Long.BYTES + Integer.BYTES + Integer.BYTES + Integer.BYTES;

    private RecordCodec() {
    }

    static byte[] encode(long offset, long timestamp, String key, byte[] value) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = value.clone();
        int bodyLength = BODY_FIXED_BYTES + keyBytes.length + valueBytes.length;
        int crc = crc(offset, timestamp, keyBytes, valueBytes);

        ByteBuffer buffer = ByteBuffer.allocate(PREFIX_BYTES + bodyLength);
        buffer.putInt(MAGIC);
        buffer.putShort(VERSION);
        buffer.putInt(bodyLength);
        buffer.putLong(offset);
        buffer.putLong(timestamp);
        buffer.putInt(keyBytes.length);
        buffer.putInt(valueBytes.length);
        buffer.putInt(crc);
        buffer.put(keyBytes);
        buffer.put(valueBytes);
        return buffer.array();
    }

    static Optional<DecodedRecord> decode(ByteBuffer recordBuffer) {
        recordBuffer.rewind();
        if (recordBuffer.remaining() < PREFIX_BYTES + BODY_FIXED_BYTES) {
            return Optional.empty();
        }
        int magic = recordBuffer.getInt();
        short version = recordBuffer.getShort();
        int bodyLength = recordBuffer.getInt();
        if (magic != MAGIC || version != VERSION || bodyLength < BODY_FIXED_BYTES || recordBuffer.remaining() != bodyLength) {
            return Optional.empty();
        }

        long offset = recordBuffer.getLong();
        long timestamp = recordBuffer.getLong();
        int keyLength = recordBuffer.getInt();
        int valueLength = recordBuffer.getInt();
        int expectedCrc = recordBuffer.getInt();
        if (keyLength < 0 || valueLength < 0 || keyLength + valueLength != recordBuffer.remaining()) {
            return Optional.empty();
        }

        byte[] keyBytes = new byte[keyLength];
        byte[] valueBytes = new byte[valueLength];
        recordBuffer.get(keyBytes);
        recordBuffer.get(valueBytes);

        int actualCrc = crc(offset, timestamp, keyBytes, valueBytes);
        if (actualCrc != expectedCrc) {
            return Optional.empty();
        }

        return Optional.of(new DecodedRecord(offset, timestamp, new String(keyBytes, StandardCharsets.UTF_8), valueBytes));
    }

    static int crc(long offset, long timestamp, byte[] keyBytes, byte[] valueBytes) {
        CRC32 crc32 = new CRC32();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            bytes.write(ByteBuffer.allocate(Long.BYTES).putLong(offset).array());
            bytes.write(ByteBuffer.allocate(Long.BYTES).putLong(timestamp).array());
            bytes.write(ByteBuffer.allocate(Integer.BYTES).putInt(keyBytes.length).array());
            bytes.write(ByteBuffer.allocate(Integer.BYTES).putInt(valueBytes.length).array());
            bytes.write(keyBytes);
            bytes.write(valueBytes);
        } catch (IOException ex) {
            throw new IllegalStateException("Unexpected in-memory CRC failure", ex);
        }
        crc32.update(bytes.toByteArray());
        return (int) crc32.getValue();
    }

    record DecodedRecord(long offset, long timestamp, String key, byte[] value) {
    }
}
