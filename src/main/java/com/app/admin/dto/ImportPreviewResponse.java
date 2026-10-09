package com.app.admin.dto;

import com.app.admin.model.ImportStatus;

public record ImportPreviewResponse(
        Long importId,
        ImportStatus status,
        ImportSummaryDto summary
) {
}

