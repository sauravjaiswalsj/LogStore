package com.projects.logstore.controller;

import com.projects.logstore.config.ClusterProperties;
import com.projects.logstore.dto.ClusterStatusDTO;
import com.projects.logstore.replication.ReplicationManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cluster")
public class ClusterStatusController {
    private final ClusterProperties clusterProperties;
    private final ReplicationManager replicationManager;

    public ClusterStatusController(ClusterProperties clusterProperties, ReplicationManager replicationManager) {
        this.clusterProperties = clusterProperties;
        this.replicationManager = replicationManager;
    }

    @GetMapping("/status")
    public ClusterStatusDTO status() {
        ClusterStatusDTO status = new ClusterStatusDTO();
        status.setNodeId(clusterProperties.getNodeId());
        status.setLeader(clusterProperties.isLeader());
        status.setAckMode(clusterProperties.getAckMode().name());
        status.setReplicationFactor(clusterProperties.getReplicationFactor());
        status.setLatestOffset(replicationManager.latestOffset());
        status.setCommitOffset(replicationManager.commitOffset());
        status.setPeers(clusterProperties.isLeader() ? replicationManager.peerStatuses() : java.util.List.of());
        return status;
    }
}
