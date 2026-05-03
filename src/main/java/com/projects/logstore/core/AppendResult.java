package com.projects.logstore.core;

public record AppendResult(String stream, int tabletId, long offset, long timestamp) {
}
