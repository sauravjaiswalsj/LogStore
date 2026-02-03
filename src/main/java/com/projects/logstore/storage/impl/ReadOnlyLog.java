package com.projects.logstore.storage.impl;

import com.projects.logstore.dto.LogRecord;
import com.projects.logstore.storage.ReadOnlyLogService;

import java.io.BufferedReader;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadOnlyLog implements ReadOnlyLogService {

    private static final Logger log = LoggerFactory.getLogger(ReadOnlyLog.class);

    public byte[] read(Path filePath, long offset, int length) {
        try(FileChannel channel = FileChannel.open(
                filePath,
                StandardOpenOption.READ
        )){
            channel.position(offset);
            byte[] data = new byte[length];
            int bytesRead = channel.read(java.nio.ByteBuffer.wrap(data));
            if (bytesRead == -1) {
                return new byte[0];
            }
            else{
                byte[] byteObjects = new byte[bytesRead];
                for (int i = 0; i < bytesRead; i++) {
                    byteObjects[i] = data[i];
                }
                return byteObjects;
            }
        }
        catch(Exception e){
            log.error("Erro ao leitura do log: " + e.getMessage());
        }
        return new byte[0];
    }

    public List<LogRecord> readFile(Path filePath, long offset, int length){
        List<LogRecord> logRecords = new ArrayList<>();
        try{
            BufferedReader reader = Files.newBufferedReader(filePath, java.nio.charset.StandardCharsets.UTF_8);
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");

                if (parts.length < 4){
                    continue;
                }

                long recordOffset = Long.parseLong(parts[0]);
                if (recordOffset >= offset) {
                    LogRecord logRecord = new LogRecord();
                    logRecord.setOffset(recordOffset);
                    logRecord.setTimestamp(Long.parseLong(parts[1]));
                    logRecord.setKey(parts[2]);
                    logRecord.setValue(parts[3]);
                    logRecords.add(logRecord);
                }

                if (logRecords.size() == length) {
                    break;
                }
            }
            reader.close();
        }catch (Exception e){
            log.error("Error: " + e.getMessage());
        }
        return logRecords;
    }
}
