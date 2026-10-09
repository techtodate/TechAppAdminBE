package com.app.admin.dto;

public record SourceFileMetadata(
        String fileName,
        String contentType,
        long fileSize
) {
}

