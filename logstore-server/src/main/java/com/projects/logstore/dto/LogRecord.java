package com.projects.logstore.dto;

import lombok.Data;

@Data
public class LogRecord {
    String stream;
    long offset;
    long timestamp;
    String key;
    String value;
}
