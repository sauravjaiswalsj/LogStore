package com.projects.logstore.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class TabletDetailDTO {
    private int tabletId;
    private String status;
    private String logFilePath;
    private boolean logFileExists;
    private long latestOffset;
    private long nextOffset;
    private long recordCount;
    private long fileSizeBytes;
    private Instant lastModifiedAt;
    private List<LogRecord> recentRecords;
}
