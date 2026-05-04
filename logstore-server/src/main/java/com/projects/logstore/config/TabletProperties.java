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
}
