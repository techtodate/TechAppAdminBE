package com.app.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TechUpdateResponse(
        Long id,
        Long techId,
        TechnologySummary technology,
        List<CategorySummary> categoryPath,
        String title,
        String content,
        String sourceUrl,
        String source,
        Long createdById,
        LocalDateTime createdAt,
        String summaryStatus) {

    public record TechnologySummary(Long id, String name) {}
    public record CategorySummary(Long id, String name) {}
}
