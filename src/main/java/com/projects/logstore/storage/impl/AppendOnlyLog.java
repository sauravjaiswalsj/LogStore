package com.projects.logstore.storage.impl;

import com.projects.logstore.model.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import com.projects.logstore.storage.AppendOnlyLogService;

public class AppendOnlyLog implements AppendOnlyLogService {

    private static final Logger log = LoggerFactory.getLogger(AppendOnlyLog.class);

    public void append(String filePath, byte[] record){
        try{
            Path path = Paths.get(filePath);

            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            try (FileChannel channel = FileChannel.open(
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            )){
                channel.write(ByteBuffer.wrap(record));
                channel.force(true);
                log.info("Successfully appended to file {}", filePath);
            }
        } catch (FileNotFoundException ex){
            log.error("File not found {}" , ex.getMessage());

        }
        catch(Exception ex){
            log.error("Unable to write{}", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}