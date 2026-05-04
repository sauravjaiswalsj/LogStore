package com.logstore.core.api;

public record LogRecord(String stream, long offset, long timestamp, String key, byte[] value) {
}
