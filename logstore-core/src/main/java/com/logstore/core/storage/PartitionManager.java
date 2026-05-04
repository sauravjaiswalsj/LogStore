package com.logstore.core.storage;

import com.logstore.core.api.LogStoreConfig;
import com.logstore.core.api.TabletInfo;
import com.logstore.core.util.HashUtil;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

public final class PartitionManager {
    private final Tablet[] tablets;

    public PartitionManager(LogStoreConfig config, Clock clock) throws IOException {
        this.tablets = new Tablet[config.partitions()];
        for (int i = 0; i < tablets.length; i++) {
            tablets[i] = new Tablet(i, config.dataDir(), config.durability(), clock);
        }
    }

    public Tablet tabletForStream(String stream) {
        return tablets[tabletIdForStream(stream)];
    }

    public Tablet tabletById(int tabletId) {
        if (tabletId < 0 || tabletId >= tablets.length) {
            throw new IllegalArgumentException("Unknown tabletId " + tabletId);
        }
        return tablets[tabletId];
    }

    public int tabletIdForStream(String stream) {
        return HashUtil.partitionFor(stream, tablets.length);
    }

    public int partitionCount() {
        return tablets.length;
    }

    public List<TabletInfo> tablets() throws IOException {
        List<TabletInfo> infos = new ArrayList<>(tablets.length);
        for (Tablet tablet : tablets) {
            infos.add(new TabletInfo(tablet.tabletId(), tablet.nextOffset(), tablet.latestOffset(), tablet.sizeBytes()));
        }
        return infos;
    }
}
