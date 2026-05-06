package com.projects.logstore.config;

import com.logstore.core.api.BackpressurePolicy;
import com.logstore.core.api.Durability;
import com.logstore.core.api.LogStore;
import com.logstore.core.api.LogStoreConfig;
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
                .durability(Durability.valueOf(tabletProperties.getDurability()))
                .batchSize(tabletProperties.getBatchSize())
                .flushIntervalMillis(tabletProperties.getFlushIntervalMillis())
                .indexInterval(tabletProperties.getIndexInterval())
                .maxSegmentBytes(tabletProperties.getMaxSegmentBytes())
                .queueCapacity(tabletProperties.getQueueCapacity())
                .backpressurePolicy(BackpressurePolicy.valueOf(tabletProperties.getBackpressurePolicy()))
                .build());
    }
}
