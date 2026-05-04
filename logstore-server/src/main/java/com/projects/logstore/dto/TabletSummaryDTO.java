package com.projects.logstore.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class TabletSummaryDTO {
    private int tabletId;
    private String status;
    private boolean logFileExists;
    private long latestOffset;
    private long recordCount;
    private long fileSizeBytes;
    private Instant lastModifiedAt;
}
