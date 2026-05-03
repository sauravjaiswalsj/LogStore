package com.projects.logstore.core;

public record LogRecord(String stream, long offset, long timestamp, String key, byte[] value) {
}
