package com.projects.logstore.controller;

// API exposed by Tablet/TabletServer to append new records efficiently (UUID + payload). Handles sequential writes, returns offset.

import com.projects.logstore.model.Data;
import com.projects.logstore.storage.AppendOnlyLogService;
import com.projects.logstore.storage.impl.AppendOnlyLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/")
public class AppendController {

    @PostMapping("/append")
    public long AppendController(@RequestBody Data data) {
        log.info("{}", data);

        AppendOnlyLog log = new AppendOnlyLog();
        return log.logWriter(data);
    }
}
