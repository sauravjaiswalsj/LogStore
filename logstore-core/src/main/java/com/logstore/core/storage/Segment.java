package com.logstore.core.storage;

import java.nio.file.Path;

record Segment(long baseOffset, Path path) {
    static Segment legacy(Path dataDir, int tabletId) {
        return new Segment(0L, dataDir.resolve("tablet-" + tabletId + ".log"));
    }

    static Segment rolling(Path dataDir, int tabletId, long baseOffset) {
        return new Segment(baseOffset, dataDir.resolve("tablet-" + tabletId).resolve(String.format("%020d.log", baseOffset)));
    }
}
