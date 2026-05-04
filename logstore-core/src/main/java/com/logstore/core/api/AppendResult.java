package com.logstore.core.api;

public record AppendResult(String stream, int tabletId, long offset, long timestamp) {
}
