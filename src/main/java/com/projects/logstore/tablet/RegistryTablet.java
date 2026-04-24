package com.projects.logstore.tablet;

import com.projects.logstore.config.TabletProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@Service
public class RegistryTablet {
    private String baseDir;
    private int totalTablets;
    private final HashMap<Integer, Tablet> tabletHashMap = new HashMap<>();
    private final TabletProperties tabletProperties;
    public RegistryTablet(TabletProperties tabletProperties) {
        this.tabletProperties = tabletProperties;
    }

    @PostConstruct
    public void init() {
        baseDir= tabletProperties.getBaseDir();
        totalTablets= tabletProperties.getTotalTablets();
        initializeTablets();
    }

    private void initializeTablets() {
        for (int tabId = 0; tabId < totalTablets; tabId++) {
           tabletHashMap.putIfAbsent(tabId, new Tablet(tabId, baseDir));
        }
    }

    public Tablet getTabletById(int tabletId) {
        return tabletHashMap.getOrDefault(tabletId, null);
    }

    public List<Tablet> getAllTablets() {
        return tabletHashMap.values()
                .stream()
                .sorted(Comparator.comparingInt(Tablet::getTabletId))
                .toList();
    }

    public int getTotalTablets() {
        return totalTablets;
    }

    public String getBaseDir() {
        return baseDir;
    }
}
