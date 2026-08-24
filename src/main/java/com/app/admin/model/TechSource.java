package com.app.admin.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tech_sources", schema = "public",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_tech_source_unique",
                columnNames = {"type", "segment_type", "url"}))
@Getter
@Setter
@NoArgsConstructor
public class TechSource {
    @Id
    @SequenceGenerator(name = "tech_sources_id_generator", sequenceName = "tech_sources_id_seq", schema = "public", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tech_sources_id_generator")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(name = "technology_id")
    private Long technologyId;

    @Column(length = 255)
    private String name;

    @Column(length = 255)
    private String url;

    @Column(length = 50)
    private String type;

    private Boolean active = true;

    @Column(name = "poll_interval_seconds")
    private Integer pollIntervalSeconds;

    @Column(name = "last_polled_at")
    private LocalDateTime lastPolledAt;

    @Column(name = "segment_type", length = 50)
    private String segmentType;

    @Column(name = "parser_type", length = 50)
    private String parserType;

    @Column(length = 50)
    private String status = "ACTIVE";

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "priority_score")
    private Integer priorityScore = 1;

    @Column(name = "next_fetch_at")
    private LocalDateTime nextFetchAt;

    @Column(name = "fetch_interval_minutes")
    private Long fetchIntervalMinutes = 60L;

    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;
}
