package com.projects.logstore.server;

import com.logstore.core.api.AppendResult;
import com.logstore.core.api.LogStore;
import com.projects.logstore.dto.AppendDTO;
import com.projects.logstore.dto.LogRecord;
import com.projects.logstore.dto.ReadDTO;
import com.projects.logstore.model.Data;
import com.projects.logstore.replication.ReplicationManager;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class TabletServer {
    private final LogStore logStore;
    private final ReplicationManager replicationManager;

    public TabletServer(LogStore logStore, ReplicationManager replicationManager) {
        this.logStore = logStore;
        this.replicationManager = replicationManager;
    }

    public AppendDTO append(Data data){
        if (!replicationManager.isLeader()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This node is a follower; send client appends to the configured leader");
        }
        byte[] valueBytes = data.getValue() != null ? data.getValue().getBytes(StandardCharsets.UTF_8) : new byte[0];
        AppendResult result = logStore.append(data.getStream(), data.getKey(), valueBytes);
        ReplicationManager.ReplicationOutcome outcome = replicationManager.replicate(result, data.getKey(), data.getValue());
        if (!outcome.success()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Append persisted on leader but quorum replication failed");
        }
        AppendDTO appendDTO = new AppendDTO();
        appendDTO.setStream(result.stream());
        appendDTO.setTabletId(result.tabletId());
        appendDTO.setOffset(result.offset());
        appendDTO.setCommitted(true);
        appendDTO.setPersistedReplicas(outcome.persistedReplicas());
        return appendDTO;
    }

    public ReadDTO read(String stream, Long startOffset, int limit ){
        List<LogRecord> records = logStore.read(stream, startOffset, limit).stream()
                .map(TabletServer::toDto)
                .toList();
        ReadDTO readDTO = new ReadDTO();
        readDTO.setStream(stream);
        readDTO.setTabletId(logStore.tabletForStreamId(stream));
        readDTO.setOffset(startOffset);
        readDTO.setLimit(limit);
        readDTO.setNextOffset(records.isEmpty() ? startOffset : records.get(records.size() - 1).getOffset() + 1L);
        readDTO.setLogRecords(records);
        return readDTO;
    }

    private static LogRecord toDto(com.logstore.core.api.LogRecord record) {
        LogRecord dto = new LogRecord();
        dto.setOffset(record.offset());
        dto.setTimestamp(record.timestamp());
        dto.setKey(record.key());
        dto.setValue(new String(record.value(), StandardCharsets.UTF_8));
        return dto;
    }
}
