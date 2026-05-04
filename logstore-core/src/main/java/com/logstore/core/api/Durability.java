package com.logstore.core.api;

public enum Durability {
    FSYNC_EVERY_WRITE,

    // Planned for V0.2. Currently accepted as configuration but treated like FSYNC_EVERY_WRITE.
    BATCHED_FSYNC,

    // Planned for V0.2. Currently accepted as configuration but treated like FSYNC_EVERY_WRITE.
    ASYNC_FLUSH
}
