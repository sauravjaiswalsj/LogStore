package com.projects.logstore.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class ReadDTO {
    int tabletId;
    long offset;
    List<LogRecord> logRecords;
}
