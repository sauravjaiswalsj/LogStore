package com.projects.logstore.dto;

import lombok.Data;

import java.util.List;

@Data
public class ConsumerBatchDTO {
    private String stream;
    private String consumerGroup;
    private int tabletId;
    private long offset;
    private long nextOffset;
    private int limit;
    private List<LogRecord> records;
}
