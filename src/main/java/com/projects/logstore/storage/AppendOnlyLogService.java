package com.projects.logstore.storage;

import com.projects.logstore.model.Data;

public interface AppendOnlyLogService {
    long logWriter(Data data);
}
