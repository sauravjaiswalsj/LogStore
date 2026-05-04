package com.logstore.core.api;

public record TabletInfo(int tabletId, long nextOffset, long latestOffset, long sizeBytes) {
}
