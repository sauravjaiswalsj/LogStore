package com.projects.logstore.storage;

public interface AppendOnlyLogService {
    void append(String filePath, byte[] record);
}
