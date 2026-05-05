package com.projects.logstore.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FetchRecordsDTO {
    private int tabletId;
    private long offset;
    private int limit;
    private long nextOffset;
    private List<ReplicationRecordDTO> records = new ArrayList<>();
}
