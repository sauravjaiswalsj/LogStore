package com.projects.logstore.storage;

import com.projects.logstore.dto.LogRecord;

import java.nio.file.Path;
import java.util.List;

public interface ReadOnlyLogService {
    public byte[] read(Path filePath, long offset, int length);
    public List<LogRecord> readFile(Path filePath, long offset, int length);
}
