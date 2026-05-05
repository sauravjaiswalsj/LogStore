package com.projects.logstore.controller;

import com.projects.logstore.dto.FetchRecordsDTO;
import com.projects.logstore.dto.ReplicationRecordDTO;
import com.projects.logstore.dto.ReplicationResultDTO;
import com.projects.logstore.replication.ReplicationManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class ReplicationController {
    private final ReplicationManager replicationManager;

    public ReplicationController(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
    }

    @PostMapping("/replicate")
    public ReplicationResultDTO replicate(@RequestBody ReplicationRecordDTO record) {
        return replicationManager.appendFromLeader(record);
    }

    @GetMapping("/fetch")
    public FetchRecordsDTO fetchFromOffset(
            @RequestParam int tabletId,
            @RequestParam long offset,
            @RequestParam(required = false, defaultValue = "1000") int limit
    ) {
        return replicationManager.fetchFromOffset(tabletId, offset, limit);
    }
}
