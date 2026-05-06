package com.projects.logstore.dto;

import lombok.Data;

@Data
public class ReplicationResultDTO {
    private boolean success;
    private long offset;
    private long expectedOffset;
    private String message;
}
