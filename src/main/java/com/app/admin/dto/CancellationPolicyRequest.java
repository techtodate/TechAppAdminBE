package com.app.admin.dto;

import jakarta.validation.constraints.NotNull;

public record CancellationPolicyRequest(@NotNull Long trainingOfferingId, @NotNull Long beforeTrainingStartPolicyId,
        Long betweenSessionsPolicyId, Long afterTrainingCompletionPolicyId, Boolean active) {}
