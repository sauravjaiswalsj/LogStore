package com.projects.logstore.controller;

import com.projects.logstore.dto.ConsumerBatchDTO;
import com.projects.logstore.dto.CursorCommitDTO;
import com.projects.logstore.server.TabletServer;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/")
public class ConsumerController {
    private final TabletServer tabletServer;

    public ConsumerController(TabletServer tabletServer) {
        this.tabletServer = tabletServer;
    }

    @GetMapping("/consume")
    public ConsumerBatchDTO consume(
            @RequestParam String stream,
            @RequestParam String consumerGroup,
            @RequestParam(required = false, defaultValue = "100") int limit
    ) {
        if (limit <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be greater than zero");
        }
        return tabletServer.poll(stream, consumerGroup, limit);
    }

    @PostMapping("/consume/commit")
    public ConsumerBatchDTO commit(@RequestBody CursorCommitDTO commit) {
        if (commit == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commit body is required");
        }
        return tabletServer.commit(commit.getStream(), commit.getConsumerGroup(), commit.getNextOffset());
    }
}
