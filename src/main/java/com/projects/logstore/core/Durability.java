package com.projects.logstore.core;

public enum Durability {
    FSYNC_EVERY_WRITE,
    BATCHED_FSYNC,
    ASYNC_FLUSH
}
