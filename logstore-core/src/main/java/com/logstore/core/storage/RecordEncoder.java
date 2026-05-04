package com.logstore.core.storage;

import com.logstore.core.util.CRCUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

final class RecordEncoder {
    static final int MAGIC = 0x4c535431;
    static final short VERSION = 1;
    static final int PREFIX_BYTES = Integer.BYTES + Short.BYTES + Integer.BYTES;
    static final int BODY_FIXED_BYTES = Long.BYTES + Long.BYTES + Integer.BYTES + Integer.BYTES + Integer.BYTES + Integer.BYTES;
    static final int MAX_RECORD_BODY_BYTES = 16 * 1024 * 1024;
    static final int MAX_FIELD_BYTES = 10 * 1024 * 1024;

    private RecordEncoder() {
    }

    static byte[] encode(long offset, long timestamp, String stream, String key, byte[] value) {
        byte[] streamBytes = stream.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = value.clone();
        if (streamBytes.length > MAX_FIELD_BYTES || keyBytes.length > MAX_FIELD_BYTES || valueBytes.length > MAX_FIELD_BYTES) {
            throw new IllegalArgumentException("record field exceeds max encoded size");
        }
        int bodyLength = BODY_FIXED_BYTES + streamBytes.length + keyBytes.length + valueBytes.length;
        if (bodyLength > MAX_RECORD_BODY_BYTES) {
            throw new IllegalArgumentException("record exceeds max encoded size");
        }
        int crc = CRCUtil.recordCrc(offset, timestamp, streamBytes, keyBytes, valueBytes);

        ByteBuffer buffer = ByteBuffer.allocate(PREFIX_BYTES + bodyLength);
        buffer.putInt(MAGIC);
        buffer.putShort(VERSION);
        buffer.putInt(bodyLength);
        buffer.putLong(offset);
        buffer.putLong(timestamp);
        buffer.putInt(streamBytes.length);
        buffer.putInt(keyBytes.length);
        buffer.putInt(valueBytes.length);
        buffer.putInt(crc);
        buffer.put(streamBytes);
        buffer.put(keyBytes);
        buffer.put(valueBytes);
        return buffer.array();
    }
}
