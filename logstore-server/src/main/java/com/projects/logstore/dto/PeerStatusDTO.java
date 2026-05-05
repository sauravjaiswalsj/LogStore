package com.projects.logstore.dto;

import lombok.Data;

@Data
public class PeerStatusDTO {
    private String peer;
    private boolean healthy;
    private long latestOffset;
    private long lag;
    private String message;
}
