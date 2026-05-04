package com.projects.logstore.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReadDTO {
    String stream;
    int tabletId;
    long offset;
    int limit;
    long nextOffset;
    List<LogRecord> logRecords;
}
