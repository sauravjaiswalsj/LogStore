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
    private String nodeId;
    private boolean leader;
    private String ackMode;
    private int replicationFactor;
    private long latestOffset;
    private long commitOffset;
    private java.util.List<PeerStatusDTO> peers;
}
