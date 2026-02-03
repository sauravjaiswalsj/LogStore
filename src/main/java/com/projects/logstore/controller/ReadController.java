package com.projects.logstore.controller;

import com.projects.logstore.dto.ReadDTO;
import com.projects.logstore.server.TabletServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/")
public class ReadController {

    private final TabletServer tabletServer;

    public ReadController(TabletServer tabletServer) {
        this.tabletServer = tabletServer;
    }
    @GetMapping("/read")
    public ReadDTO ReadController(@RequestParam Integer tabletId, @RequestParam Long startOffset, @RequestParam(required = false) Integer limit) {
        if (tabletId == null) {
            throw new IllegalArgumentException("Tablet ID cannot be null");
        }
        if (startOffset == null || startOffset < 0) {
            throw new IllegalArgumentException("Start offset cannot be null or negative");
        }

        // TODO: Find a better way to handle default limit
        if (limit == null || limit < 0) {
            limit = 100; // default limit
        }

        log.info("Read request for tabletId: {}, startOffset: {}, limit: {}", tabletId, startOffset, limit);

        return tabletServer.read(tabletId, startOffset, limit);
    }
}
