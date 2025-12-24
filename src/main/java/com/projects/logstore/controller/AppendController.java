package com.projects.logstore.controller;

// API exposed by Tablet/TabletServer to append new records efficiently (UUID + payload). Handles sequential writes, returns offset.

import com.projects.logstore.model.Data;
import com.projects.logstore.server.TabletServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/")
public class AppendController {

    private final TabletServer tabletServer;

    public AppendController(TabletServer tabletServer) {
        this.tabletServer = tabletServer;
    }

    @PostMapping("/append")
    public long AppendController(@RequestBody Data data) {
        if (data == null){
            throw new IllegalArgumentException("Data cannot be null");
        }
        if (data.getKey() == null || data.getKey().isEmpty()){
            throw new IllegalArgumentException("Data key cannot be null or empty");
        }
        if (data.getValue() == null){
            throw new IllegalArgumentException("Data value cannot be null");
        }
        log.info("{}", data);
       return  tabletServer.append(data);
    }
}
