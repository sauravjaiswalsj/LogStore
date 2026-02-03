package com.projects.logstore.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReadDTO {
    int tabletId;
    long offset;
    List<LogRecord> logRecords;
}
