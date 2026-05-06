package com.projects.logstore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tablet")
@Data
public class TabletProperties {
    private String baseDir;

    private int totalTablets;

    private String durability = "FSYNC_EVERY_WRITE";

    private int batchSize = 128;

    private long flushIntervalMillis = 5L;

    private int indexInterval = 128;

    private long maxSegmentBytes = Long.MAX_VALUE;

    private int queueCapacity = 8192;

    private String backpressurePolicy = "BLOCK";
}
