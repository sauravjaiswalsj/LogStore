package com.projects.logstore.controller;

import com.projects.logstore.dto.ReadDTO;
import com.projects.logstore.server.TabletServer;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/")
public class ReadController {

    private final TabletServer tabletServer;

    public ReadController(TabletServer tabletServer) {
        this.tabletServer = tabletServer;
    }
    @GetMapping("/read")
    @Tag(name = "Read API to fetch log records from a stream starting from a given offset with an optional limit.")
    @Parameter(name = "stream", description = "The stream to read from", required = true)
    @Parameter(name = "offset", description = "The starting offset to read from", required = false)
    @Parameter(name = "startOffset", description = "Legacy alias for offset", required = false)
    @Parameter(name = "limit", description = "The maximum number of records to read", required = false)
    public ReadDTO readLog(
            @RequestParam String stream,
            @RequestParam(required = false) Long offset,
            @RequestParam(required = false) Long startOffset,
            @RequestParam(required = false) Integer limit
    ) {
        if (stream == null || stream.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stream cannot be null or empty");
        }
        Long resolvedOffset = offset != null ? offset : startOffset;
        if (resolvedOffset == null || resolvedOffset < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offset cannot be null or negative");
        }

        if (limit == null) {
            limit = 10;
        } else if (limit <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be greater than zero");
        }

        log.info("Read request for stream: {}, offset: {}, limit: {}", stream, resolvedOffset, limit);

        return tabletServer.read(stream, resolvedOffset, limit);
    }
}
