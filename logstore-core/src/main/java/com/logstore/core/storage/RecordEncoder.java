package com.logstore.core.storage;

import com.logstore.core.util.CRCUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

final class RecordEncoder {
    static final int MAGIC = 0x4c535431;
    static final short VERSION = 1;
    static final int PREFIX_BYTES = Integer.BYTES + Short.BYTES + Integer.BYTES;
    static final int BODY_FIXED_BYTES = Long.BYTES + Long.BYTES + Integer.BYTES + Integer.BYTES + Integer.BYTES + Integer.BYTES;

    private RecordEncoder() {
    }

    static byte[] encode(long offset, long timestamp, String stream, String key, byte[] value) {
        byte[] streamBytes = stream.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = value.clone();
        int bodyLength = BODY_FIXED_BYTES + streamBytes.length + keyBytes.length + valueBytes.length;
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
