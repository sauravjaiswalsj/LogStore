package com.logstore.core.api;

public enum Durability {
    FSYNC_EVERY_WRITE,

    BATCHED_FSYNC,

    ASYNC_FLUSH
}
