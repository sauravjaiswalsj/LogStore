package com.projects.logstore.dto;

import lombok.Data;

@Data
public class ClusterOverviewDTO {
    private String status;
    private String topologyMode;
    private String leaderElection;
    private String replication;
    private int totalTablets;
    private String note;
}
