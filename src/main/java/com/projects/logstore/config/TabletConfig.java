package com.projects.logstore.config;

import com.projects.logstore.tablet.RegistryTablet;
import com.projects.logstore.tablet.TabletRouter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TabletConfig {
    @Bean
    public TabletRouter tabletRouter(TabletProperties tabletProperties) {
        return new TabletRouter(tabletProperties.getTotalTablets());
    }

    @Bean
    public RegistryTablet registryTablet(TabletProperties tabletProperties) {
        return new RegistryTablet(tabletProperties);
    }


}
