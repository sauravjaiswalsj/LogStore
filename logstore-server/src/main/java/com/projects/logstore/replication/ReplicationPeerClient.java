package com.projects.logstore.replication;

import com.projects.logstore.dto.ClusterStatusDTO;
import com.projects.logstore.dto.FetchRecordsDTO;
import com.projects.logstore.dto.ReplicationRecordDTO;
import com.projects.logstore.dto.ReplicationResultDTO;

public interface ReplicationPeerClient {
    ReplicationResultDTO replicate(String peer, ReplicationRecordDTO record);

    FetchRecordsDTO fetchFromOffset(String peer, int tabletId, long offset, int limit);

    ClusterStatusDTO status(String peer);
}
