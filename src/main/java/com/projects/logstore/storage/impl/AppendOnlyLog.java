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
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import com.projects.logstore.storage.AppendOnlyLogService;

public class AppendOnlyLog implements AppendOnlyLogService {

    private static final Logger log = LoggerFactory.getLogger(AppendOnlyLog.class);
    private static final String filePath = "data/logstore.log";
    
    private final AtomicLong nextOffset;
    
    public AppendOnlyLog() {
        this.nextOffset = new AtomicLong(recoverNextOffset());
        log.info("AppendOnlyLog initialized with nextOffset: {}", nextOffset.get());
    }
    
    public long logWriter(Data data){
        try{
            
            Path path = Paths.get(filePath);

            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            try (FileChannel channel = FileChannel.open(
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            )) {
                long offSetAfterWrite = nextOffset.getAndIncrement(); // assign offset automatically
                
                long time = System.currentTimeMillis();
                
                String record = offSetAfterWrite + "|" + 
                                time + "|" + 
                                data.getKey() + "|" + 
                                data.getValue() + "\n";
                // Append to file
                channel.write(ByteBuffer.wrap(record.getBytes()));
                // Durability Gaurantee (fsync)
                channel.force(true);

                log.info("Successfully appended to file at offset {}", offSetAfterWrite);
                return offSetAfterWrite;
            }
        }catch (FileNotFoundException ex){
            log.error("File not found {}" , ex.getMessage());
        }
        catch(Exception ex){
            log.error("Unable to write{}", ex.getMessage());
        }
        return -1;
    }
}