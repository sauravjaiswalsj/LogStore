package com.projects.logstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class PingController {
    private static final Logger log = LoggerFactory.getLogger(PingController.class);

    @GetMapping("/ping")
    public String ping() {
        long startTime = System.currentTimeMillis();
        log.info("START | endpoint=/ping | thread={} ", Thread.currentThread().getName());

        String response = "success";

        long duration = System.currentTimeMillis() - startTime;
        log.info("END   | endpoint=/ping | thread={} | duration={}ms | response={}",
                Thread.currentThread().getName(), duration, response);

        return response;

    }
}
