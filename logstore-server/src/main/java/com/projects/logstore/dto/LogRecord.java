package com.projects.logstore.dto;

import lombok.Data;

@Data
public class LogRecord {
    long offset;
    long timestamp;
    String key;
    String value;
}
