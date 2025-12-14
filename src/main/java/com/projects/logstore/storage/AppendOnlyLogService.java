package com.projects.logstore.storage;

import com.projects.logstore.model.Data;
import org.springframework.stereotype.Service;

@Service
public interface AppendOnlyLogService {
    long logWriter(Data data);
}
