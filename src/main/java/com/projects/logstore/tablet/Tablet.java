package com.projects.logstore.tablet;

import com.projects.logstore.storage.impl.AppendOnlyLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(Tablet.class);

    public Tablet(int tabletId, String logFilePath, AtomicLong nextOffset, AppendOnlyLog appendOnlyLog) {
        this.tabletId = tabletId;
        this.logFilePath = logFilePath;
        this.nextOffset = nextOffset;
        this.appendOnlyLog = appendOnlyLog;
    }

    public Tablet(int tabletId, String baseDir){
        this.tabletId = tabletId;
        this.logFilePath = baseDir + "tablet-" + tabletId + ".log";
        this.appendOnlyLog = new AppendOnlyLog();
        this.nextOffset = new AtomicLong(recoverNextOffset());
    }

    public long append(String key, String value){
        long offset = nextOffset.get();
        long timestamp = System.currentTimeMillis();

        String record = offset + "|" +
                        timestamp + "|" +
                        key + "|" +
                        value + "\n";

        appendOnlyLog.append(logFilePath, record.getBytes());
        return offset;
    }

    private long recoverNextOffset() {
        Path path = Paths.get(logFilePath);
        if (!Files.exists(path)) {
            log.info("File {} does not exist", logFilePath);
            return 0L;
        }

        try{
            List<String> lines = Files.readAllLines(path);

            if (lines.isEmpty()) {
                return 0L;
            }

            String lastLine = lines.get(lines.size() - 1);
            String[] parts = lastLine.split("\\|");

            long lastOffSet = Long.parseLong(parts[0]);
            return lastOffSet + 1;
        } catch(Exception ex){
            log.error("Failed to recover next offset: {}", ex.getMessage());
            return 0L;
        }
    }
}