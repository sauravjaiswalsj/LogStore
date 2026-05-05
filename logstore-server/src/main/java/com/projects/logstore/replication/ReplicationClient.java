package com.projects.logstore.replication;

import com.projects.logstore.dto.ClusterStatusDTO;
import com.projects.logstore.dto.ReplicationRecordDTO;
import com.projects.logstore.dto.ReplicationResultDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ReplicationClient {
    private final RestClient restClient = RestClient.create();

    public ReplicationResultDTO replicate(String peer, ReplicationRecordDTO record) {
        return restClient.post()
                .uri(normalize(peer) + "/internal/replicate")
                .body(record)
                .retrieve()
                .body(ReplicationResultDTO.class);
    }

    public ClusterStatusDTO status(String peer) {
        return restClient.get()
                .uri(normalize(peer) + "/cluster/status")
                .retrieve()
                .body(ClusterStatusDTO.class);
    }

    private static String normalize(String peer) {
        if (peer.startsWith("http://") || peer.startsWith("https://")) {
            return peer;
        }
        return "http://" + peer;
    }
}
