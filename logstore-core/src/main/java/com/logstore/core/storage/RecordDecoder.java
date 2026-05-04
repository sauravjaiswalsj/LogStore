package com.logstore.core.storage;

import com.logstore.core.util.CRCUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

final class RecordDecoder {
    private RecordDecoder() {
    }

    static Optional<DecodedRecord> decode(ByteBuffer recordBuffer) {
        recordBuffer.rewind();
        if (recordBuffer.remaining() < RecordEncoder.PREFIX_BYTES + RecordEncoder.BODY_FIXED_BYTES) {
            return Optional.empty();
        }
        int magic = recordBuffer.getInt();
        short version = recordBuffer.getShort();
        int bodyLength = recordBuffer.getInt();
        if (magic != RecordEncoder.MAGIC || version != RecordEncoder.VERSION || bodyLength < RecordEncoder.BODY_FIXED_BYTES || recordBuffer.remaining() != bodyLength) {
            return Optional.empty();
        }

        long offset = recordBuffer.getLong();
        long timestamp = recordBuffer.getLong();
        int streamLength = recordBuffer.getInt();
        int keyLength = recordBuffer.getInt();
        int valueLength = recordBuffer.getInt();
        int expectedCrc = recordBuffer.getInt();
        if (streamLength < 0 || keyLength < 0 || valueLength < 0 || streamLength + keyLength + valueLength != recordBuffer.remaining()) {
            return Optional.empty();
        }

        byte[] streamBytes = new byte[streamLength];
        byte[] keyBytes = new byte[keyLength];
        byte[] valueBytes = new byte[valueLength];
        recordBuffer.get(streamBytes);
        recordBuffer.get(keyBytes);
        recordBuffer.get(valueBytes);

        int actualCrc = CRCUtil.recordCrc(offset, timestamp, streamBytes, keyBytes, valueBytes);
        if (actualCrc != expectedCrc) {
            return Optional.empty();
        }

        return Optional.of(new DecodedRecord(
                offset,
                timestamp,
                new String(streamBytes, StandardCharsets.UTF_8),
                new String(keyBytes, StandardCharsets.UTF_8),
                valueBytes
        ));
    }

    record DecodedRecord(long offset, long timestamp, String stream, String key, byte[] value) {
    }
}
