package com.app.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminUserRoleAssignmentRequest(@NotNull Long roleId) {}
