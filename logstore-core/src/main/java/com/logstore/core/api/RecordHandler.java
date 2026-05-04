package com.logstore.core.api;

@FunctionalInterface
public interface RecordHandler {
    void onRecord(LogRecord record);
}
