package com.app.admin.dto;

import java.util.List;

public record FinancialListResponse(List<FinancialRecordResponse> items, int count, int limit) {}
