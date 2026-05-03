package com.projects.logstore.server;

import com.projects.logstore.core.AppendResult;
import com.projects.logstore.core.LogStore;
import com.projects.logstore.dto.AppendDTO;
import com.projects.logstore.dto.LogRecord;
import com.projects.logstore.dto.ReadDTO;
import com.projects.logstore.model.Data;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class TabletServer {
    private static final String DEFAULT_STREAM = "default";
    private final LogStore logStore;

    public TabletServer(LogStore logStore) {
        this.logStore = logStore;
    }

    public AppendDTO append(Data data){
        AppendResult result = logStore.append(DEFAULT_STREAM, data.getKey(), data.getValue().getBytes(StandardCharsets.UTF_8));
        AppendDTO appendDTO = new AppendDTO();
        appendDTO.setTabletId(result.tabletId());
        appendDTO.setOffset(result.offset());
        return appendDTO;
    }

    public ReadDTO read(int tabletId, Long startOffset, int limit ){
        List<LogRecord> records = logStore.readTablet(tabletId, startOffset, limit).stream()
                .map(TabletServer::toDto)
                .toList();
        ReadDTO readDTO = new ReadDTO();
        readDTO.setTabletId(tabletId);
        readDTO.setOffset(startOffset);
        readDTO.setLogRecords(records);
        return readDTO;
    }

    private static LogRecord toDto(com.projects.logstore.core.LogRecord record) {
        LogRecord dto = new LogRecord();
        dto.setOffset(record.offset());
        dto.setTimestamp(record.timestamp());
        dto.setKey(record.key());
        dto.setValue(new String(record.value(), StandardCharsets.UTF_8));
        return dto;
    }
}
