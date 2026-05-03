package com.projects.logstore.config;

import com.projects.logstore.core.Durability;
import com.projects.logstore.core.LogStore;
import com.projects.logstore.core.LogStoreConfig;
import com.projects.logstore.tablet.TabletRouter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class TabletConfig {
    @Bean
    public TabletRouter tabletRouter(TabletProperties tabletProperties) {
        return new TabletRouter(tabletProperties.getTotalTablets());
    }

    @Bean
    public LogStore logStore(TabletProperties tabletProperties) {
        return LogStore.open(LogStoreConfig.builder()
                .dataDir(Path.of(tabletProperties.getBaseDir()))
                .partitions(tabletProperties.getTotalTablets())
                .durability(Durability.FSYNC_EVERY_WRITE)
                .build());
    }
}
