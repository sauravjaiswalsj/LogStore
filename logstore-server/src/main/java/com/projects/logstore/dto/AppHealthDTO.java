package com.projects.logstore.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class AppHealthDTO {
    private String status;
    private String appName;
    private Instant timestamp;
    private int totalTablets;
    private int availableLogs;
    private String mode;
}
