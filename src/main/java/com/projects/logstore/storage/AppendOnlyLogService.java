package com.projects.logstore.storage;

import com.projects.logstore.model.Data;

public interface AppendOnlyLogService {
    void append(String filePath, byte[] record);
}
