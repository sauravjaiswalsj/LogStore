package com.logstore.core.storage;

import java.nio.file.Path;

final class Segment {
    private final Path path;

    Segment(Path dataDir, int tabletId) {
        this.path = dataDir.resolve("tablet-" + tabletId + ".log");
    }

    Path path() {
        return path;
    }
}
