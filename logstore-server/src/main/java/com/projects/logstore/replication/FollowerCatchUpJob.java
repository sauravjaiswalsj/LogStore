package com.projects.logstore.replication;

import com.projects.logstore.config.ClusterProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FollowerCatchUpJob {
    private final ClusterProperties clusterProperties;
    private final ReplicationManager replicationManager;

    public FollowerCatchUpJob(ClusterProperties clusterProperties, ReplicationManager replicationManager) {
        this.clusterProperties = clusterProperties;
        this.replicationManager = replicationManager;
    }

    @Scheduled(fixedDelayString = "${cluster.catch-up-interval-millis:2000}")
    public void catchUp() {
        if (clusterProperties.isLeader() || !clusterProperties.isFollowerCatchUpEnabled()) {
            return;
        }
        ReplicationManager.CatchUpResult result = replicationManager.catchUpFromLeader();
        if (!result.success()) {
            log.debug("Follower catch-up skipped/failed: {}", result.message());
        } else if (result.appliedRecords() > 0) {
            log.info("Follower catch-up applied {} records", result.appliedRecords());
        }
    }
}
