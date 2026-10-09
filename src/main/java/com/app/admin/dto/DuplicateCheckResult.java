package com.app.admin.dto;

import java.util.List;
import com.app.admin.model.RecordClassification;

public record DuplicateCheckResult(
        RecordClassification classification,
        Long targetInstitutionId,
        List<MatchCandidate> matches
) {
    public static DuplicateCheckResult of(RecordClassification classification, Long targetInstitutionId) {
        return new DuplicateCheckResult(classification, targetInstitutionId, List.of());
    }

    public static DuplicateCheckResult duplicate(List<MatchCandidate> matches) {
        return new DuplicateCheckResult(RecordClassification.DUPLICATE, null, matches);
    }
}
