package com.projects.logstore.tablet;

import com.projects.logstore.storage.impl.AppendOnlyLog;

import java.util.concurrent.atomic.AtomicLong;

public class Tablet {
    private final int tabletId;
    private final String logFilePath;
    private final AtomicLong nextOffset;
    private final AppendOnlyLog appendOnlyLog;

    pubic Tablet
}


/*
package com.example.logstore.tablet;

import com.example.logstore.storage.AppendOnlyLog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Tablet {

    private final int tabletId;
    private final String logFilePath;
    private final AtomicLong nextOffset;
    private final AppendOnlyLog appendOnlyLog;

    public Tablet(int tabletId, String baseDir) {
        this.tabletId = tabletId;
        this.logFilePath = baseDir + "/tablet-" + tabletId + ".log";
        this.appendOnlyLog = new AppendOnlyLog();
        this.nextOffset = new AtomicLong(recoverNextOffset());
    }

    public long append(String key, String value) {
        long offset = nextOffset.getAndIncrement();
        long timestamp = System.currentTimeMillis();

        String record =
                offset + "|" +
                timestamp + "|" +
                key + "|" +
                value + "\n";

        appendOnlyLog.append(logFilePath, record.getBytes());
        return offset;
    }

    private long recoverNextOffset() {
        Path path = Paths.get(logFilePath);

        if (!Files.exists(path)) {
            return 0;
        }

        try {
            List<String> lines = Files.readAllLines(path);
            if (lines.isEmpty()) {
                return 0;
            }

            String lastLine = lines.get(lines.size() - 1);
            String[] parts = lastLine.split("\\|");

            long lastOffset = Long.parseLong(parts[0]);
            return lastOffset + 1;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to recover offset for tablet " + tabletId, e
            );
        }
    }

    public int getTabletId() {
        return tabletId;
    }
}

 */
