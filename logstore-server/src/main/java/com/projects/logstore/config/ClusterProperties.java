package com.projects.logstore.config;

import com.projects.logstore.cluster.AckMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "cluster")
@Data
public class ClusterProperties {
    private String nodeId = "node-1";
    private boolean leader = true;
    private int replicationFactor = 1;
    private AckMode ackMode = AckMode.LEADER_ONLY;
    private List<String> peers = new ArrayList<>();
    private String leaderUrl = "";
    private int grpcPort = 9091;
    private boolean grpcEnabled = true;
    private boolean followerCatchUpEnabled = true;
    private int catchUpBatchSize = 1000;
    private long catchUpIntervalMillis = 2000L;
}
