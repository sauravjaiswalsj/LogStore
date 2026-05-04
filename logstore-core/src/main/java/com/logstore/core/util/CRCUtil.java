package com.logstore.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public final class CRCUtil {
    private CRCUtil() {
    }

    public static int recordCrc(long offset, long timestamp, byte[] streamBytes, byte[] keyBytes, byte[] valueBytes) {
        CRC32 crc32 = new CRC32();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            bytes.write(ByteBuffer.allocate(Long.BYTES).putLong(offset).array());
            bytes.write(ByteBuffer.allocate(Long.BYTES).putLong(timestamp).array());
            bytes.write(ByteBuffer.allocate(Integer.BYTES).putInt(streamBytes.length).array());
            bytes.write(ByteBuffer.allocate(Integer.BYTES).putInt(keyBytes.length).array());
            bytes.write(ByteBuffer.allocate(Integer.BYTES).putInt(valueBytes.length).array());
            bytes.write(streamBytes);
            bytes.write(keyBytes);
            bytes.write(valueBytes);
        } catch (IOException ex) {
            throw new IllegalStateException("Unexpected in-memory CRC failure", ex);
        }
        crc32.update(bytes.toByteArray());
        return (int) crc32.getValue();
    }
}
