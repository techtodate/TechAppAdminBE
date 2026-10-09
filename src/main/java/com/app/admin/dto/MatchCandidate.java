package com.app.admin.dto;

import java.util.Map;
import com.app.admin.model.MatchConfidence;

public record MatchCandidate(
        Long institutionId,
        String institutionName,
        int score,
        MatchConfidence confidence,
        Map<String, Object> reason
) {
}

