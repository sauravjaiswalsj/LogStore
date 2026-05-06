package com.projects.logstore.dto;

import lombok.Data;

@Data
public class CursorCommitDTO {
    private String stream;
    private String consumerGroup;
    private long nextOffset;
}
