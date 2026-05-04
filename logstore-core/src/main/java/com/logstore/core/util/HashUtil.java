package com.logstore.core.util;

import java.util.Objects;

public final class HashUtil {
    private HashUtil() {
    }

    public static int partitionFor(String stream, int partitions) {
        if (partitions <= 0) {
            throw new IllegalArgumentException("partitions must be greater than zero");
        }
        return Math.floorMod(Objects.requireNonNull(stream, "stream").hashCode(), partitions);
    }
}
