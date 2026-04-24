package com.projects.logstore.controller;

import com.projects.logstore.dto.AppHealthDTO;
import com.projects.logstore.dto.ClusterOverviewDTO;
import com.projects.logstore.dto.TabletDetailDTO;
import com.projects.logstore.dto.TabletSummaryDTO;
import com.projects.logstore.tablet.RegistryTablet;
import com.projects.logstore.tablet.Tablet;
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
    private final RegistryTablet registryTablet;

    public ObservabilityController(RegistryTablet registryTablet) {
        this.registryTablet = registryTablet;
    }

    @GetMapping("/health")
    public AppHealthDTO health() {
        List<Tablet> tablets = registryTablet.getAllTablets();

        AppHealthDTO dto = new AppHealthDTO();
        dto.setStatus("UP");
        dto.setAppName("LogStore");
        dto.setTimestamp(Instant.now());
        dto.setTotalTablets(tablets.size());
        dto.setAvailableLogs((int) tablets.stream().filter(Tablet::logFileExists).count());
        dto.setMode("single-node local development");
        return dto;
    }

    @GetMapping("/tablets")
    public List<TabletSummaryDTO> tablets() {
        return registryTablet.getAllTablets().stream()
                .map(this::toSummary)
                .toList();
    }

    @GetMapping("/tablets/{tabletId}")
    public TabletDetailDTO tabletDetail(
            @PathVariable int tabletId,
            @RequestParam(required = false, defaultValue = "12") int recentLimit
    ) {
        Tablet tablet = registryTablet.getTabletById(tabletId);
        if (tablet == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tablet not found");
        }

        TabletDetailDTO dto = new TabletDetailDTO();
        dto.setTabletId(tablet.getTabletId());
        dto.setStatus(resolveStatus(tablet));
        dto.setLogFilePath(tablet.getLogFilePath());
        dto.setLogFileExists(tablet.logFileExists());
        dto.setLatestOffset(tablet.getLatestOffset());
        dto.setNextOffset(tablet.getNextOffset());
        dto.setRecordCount(tablet.getRecordCount());
        dto.setFileSizeBytes(tablet.getFileSizeBytes());
        dto.setLastModifiedAt(tablet.getLastModifiedTime());

        long startOffset = Math.max(0L, tablet.getLatestOffset() - Math.max(0, recentLimit - 1));
        dto.setRecentRecords(tablet.getLatestOffset() >= 0
                ? tablet.read(startOffset, recentLimit).getLogRecords()
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
        dto.setTotalTablets(registryTablet.getTotalTablets());
        dto.setNote("The UI is cluster-aware, but live leader/follower topology is not exposed by the backend yet.");
        return dto;
    }

    private TabletSummaryDTO toSummary(Tablet tablet) {
        TabletSummaryDTO dto = new TabletSummaryDTO();
        dto.setTabletId(tablet.getTabletId());
        dto.setStatus(resolveStatus(tablet));
        dto.setLogFileExists(tablet.logFileExists());
        dto.setLatestOffset(tablet.getLatestOffset());
        dto.setRecordCount(tablet.getRecordCount());
        dto.setFileSizeBytes(tablet.getFileSizeBytes());
        dto.setLastModifiedAt(tablet.getLastModifiedTime());
        return dto;
    }

    private String resolveStatus(Tablet tablet) {
        if (!tablet.logFileExists()) {
            return "idle";
        }
        if (tablet.getRecordCount() == 0) {
            return "empty";
        }
        return "active";
    }
}
