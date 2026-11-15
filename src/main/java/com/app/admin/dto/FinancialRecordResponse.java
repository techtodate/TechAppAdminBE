package com.app.admin.dto;

import java.util.Map;

public record FinancialRecordResponse(Long id, Map<String, Object> fields) {}
