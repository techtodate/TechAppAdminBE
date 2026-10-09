package com.app.admin.dto;

import java.util.Map;

public record SourceRow(
        int rowIndex,
        Map<String, String> values
) {
}

