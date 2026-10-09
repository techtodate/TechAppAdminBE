package com.app.admin.dto;

public record ImportSummaryDto(
        int totalRecords,
        int validRecords,
        int newCount,
        int updatedCount,
        int unchangedCount,
        int duplicateCount,
        int errorCount
) {
}

