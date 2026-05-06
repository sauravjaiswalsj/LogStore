package com.projects.logstore.dto;

import lombok.Data;

@Data
public class ReplicationRecordDTO {
    private String stream;
    private String key;
    private String value;
    private long offset;
    private long timestamp;
}
