package com.logstore.core.api;

import java.util.List;

public record ConsumerBatch(String stream, String consumerGroup, long offset, long nextOffset, List<LogRecord> records) {
}
