package com.projects.logstore.controller;

import com.logstore.core.api.LogStore;
import com.logstore.core.api.TabletInfo;
import com.projects.logstore.dto.AppHealthDTO;
import com.projects.logstore.dto.ClusterOverviewDTO;
import com.projects.logstore.dto.LogRecord;
import com.projects.logstore.dto.TabletDetailDTO;
import com.projects.logstore.dto.TabletSummaryDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/")
public class ObservabilityController {
    private final LogStore logStore;

    public ObservabilityController(LogStore logStore) {
        this.logStore = logStore;
    }

    @GetMapping("/health")
    public AppHealthDTO health() {
        List<TabletInfo> tablets = logStore.tablets();

        AppHealthDTO dto = new AppHealthDTO();
        dto.setStatus("UP");
        dto.setAppName("LogStore");
        dto.setTimestamp(Instant.now());
        dto.setTotalTablets(tablets.size());
        dto.setAvailableLogs((int) tablets.stream().filter(tablet -> tablet.sizeBytes() > 0).count());
        dto.setMode("single-node local development");
        return dto;
    }

    @GetMapping("/tablets")
    public List<TabletSummaryDTO> tablets() {
        return logStore.tablets().stream()
                .map(this::toSummary)
                .toList();
    }

    @GetMapping("/tablets/{tabletId}")
    public TabletDetailDTO tabletDetail(
            @PathVariable int tabletId,
            @RequestParam(required = false, defaultValue = "12") int recentLimit
    ) {
        if (tabletId < 0 || tabletId >= logStore.partitionCount()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tablet not found");
        }
        TabletInfo tablet = logStore.tablets().get(tabletId);

        TabletDetailDTO dto = new TabletDetailDTO();
        dto.setTabletId(tablet.tabletId());
        dto.setStatus(resolveStatus(tablet));
        dto.setLogFilePath("managed by logstore-core");
        dto.setLogFileExists(tablet.sizeBytes() > 0);
        dto.setLatestOffset(tablet.latestOffset());
        dto.setNextOffset(tablet.nextOffset());
        dto.setRecordCount(tablet.nextOffset());
        dto.setFileSizeBytes(tablet.sizeBytes());
        dto.setLastModifiedAt(null);

        long startOffset = Math.max(0L, tablet.latestOffset() - Math.max(0, recentLimit - 1));
        dto.setRecentRecords(tablet.latestOffset() >= 0
                ? toDtoRecords(logStore.readTablet(tabletId, startOffset, recentLimit))
                : List.of());
        return dto;
    }

    @GetMapping("/cluster")
    public ClusterOverviewDTO cluster() {
        ClusterOverviewDTO dto = new ClusterOverviewDTO();
        dto.setStatus("DEGRADED");
        dto.setTopologyMode("single-node / multi-tablet");
        dto.setLeaderElection("backend pending");
        dto.setReplication("backend pending");
        dto.setTotalTablets(logStore.partitionCount());
        dto.setNote("The UI is cluster-aware, but live leader/follower topology is not exposed by the backend yet.");
        return dto;
    }

    private TabletSummaryDTO toSummary(TabletInfo tablet) {
        TabletSummaryDTO dto = new TabletSummaryDTO();
        dto.setTabletId(tablet.tabletId());
        dto.setStatus(resolveStatus(tablet));
        dto.setLogFileExists(tablet.sizeBytes() > 0);
        dto.setLatestOffset(tablet.latestOffset());
        dto.setRecordCount(tablet.nextOffset());
        dto.setFileSizeBytes(tablet.sizeBytes());
        dto.setLastModifiedAt(null);
        return dto;
    }

    private String resolveStatus(TabletInfo tablet) {
        if (tablet.sizeBytes() == 0) {
            return "idle";
        }
        if (tablet.nextOffset() == 0) {
            return "empty";
        }
        return "active";
    }

    private static List<LogRecord> toDtoRecords(List<com.logstore.core.api.LogRecord> records) {
        return records.stream()
                .map(record -> {
                    LogRecord dto = new LogRecord();
                    dto.setOffset(record.offset());
                    dto.setTimestamp(record.timestamp());
                    dto.setKey(record.key());
                    dto.setValue(new String(record.value(), java.nio.charset.StandardCharsets.UTF_8));
                    return dto;
                })
                .toList();
    }
}
