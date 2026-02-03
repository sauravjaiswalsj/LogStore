package com.projects.logstore.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class LogRecord {
    long offset;
    long timestamp;
    String key;
    String value;
}
