package com.logstore.core.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

public final class CursorStore {
    private final Path cursorDir;

    public CursorStore(Path dataDir) throws IOException {
        this.cursorDir = dataDir.resolve("_cursors");
        Files.createDirectories(cursorDir);
    }

    public synchronized long offset(String stream, String consumerGroup) throws IOException {
        Path path = pathFor(stream, consumerGroup);
        if (!Files.exists(path)) {
            return 0L;
        }
        String value = Files.readString(path, StandardCharsets.UTF_8).trim();
        if (value.isEmpty()) {
            return 0L;
        }
        try {
            long offset = Long.parseLong(value);
            return Math.max(0L, offset);
        } catch (NumberFormatException ex) {
            throw new IOException("invalid consumer cursor at " + path, ex);
        }
    }

    public synchronized void commit(String stream, String consumerGroup, long nextOffset) throws IOException {
        if (nextOffset < 0) {
            throw new IllegalArgumentException("nextOffset cannot be negative");
        }
        Path path = pathFor(stream, consumerGroup);
        Files.createDirectories(path.getParent());
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(tmp, Long.toString(nextOffset), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            Files.move(tmp, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Files.move(tmp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path pathFor(String stream, String consumerGroup) {
        return cursorDir.resolve(encode(stream)).resolve(encode(consumerGroup) + ".offset");
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
