package com.projects.logstore.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ClusterStatusDTO {
    private String nodeId;
    private boolean leader;
    private String ackMode;
    private int replicationFactor;
    private long latestOffset;
    private long commitOffset;
    private List<PeerStatusDTO> peers = new ArrayList<>();
}
