package com.projects.logstore.storage.impl;

import com.projects.logstore.model.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.projects.logstore.storage.AppendOnlyLogService;
import org.springframework.stereotype.Service;


@Service
public class AppendOnlyLog implements AppendOnlyLogService {

    private static final Logger log = LoggerFactory.getLogger(AppendOnlyLog.class);
    private long nextOffset = 0l;
    public long logWriter(Data data){
        try{
            String filePath = "./data/logstore.log";
            Path path = Paths.get(filePath);

            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            try (FileChannel channel = FileChannel.open(
                    Paths.get(filePath),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {

                long filePosition = channel.size(); // last commit offset
                channel.position(filePosition);
                channel.position(); // where the next write should happen
                if (filePosition == 0){
                    nextOffset = 0l;
                }
                long time = System.currentTimeMillis();
                String record = nextOffset + "|" + time + "|" + data.getKey() + "|" + data.getValue() + "\n";
                channel.write(ByteBuffer.wrap(record.getBytes()));

                // ToDo: Make this atomic with fsync
                channel.force(true); // ensure data is written to disk
                ++nextOffset;

                log.info("Successfully appended to file at offset {}", nextOffset);
            }

            return nextOffset -1;

        }catch (FileNotFoundException ex){
            log.error("File not found {}" , ex.getMessage());
        }
        catch(Exception ex){
            log.error("Unable to write{}", ex.getMessage());
        }
        return -1;
    }
}