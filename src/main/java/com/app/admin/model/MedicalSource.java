package com.app.admin.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medical_sources", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class MedicalSource {
    @Id
    @SequenceGenerator(name = "medical_sources_id_generator", sequenceName = "medical_sources_id_seq", schema = "public", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "medical_sources_id_generator")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String type;

    private Boolean active = true;

    @Column(name = "poll_interval_seconds")
    private Integer pollIntervalSeconds = 3600;

    @Column(name = "last_polled_at")
    private LocalDateTime lastPolledAt;

    @Column(name = "segment_type", length = 50)
    private String segmentType;

    @Column(name = "parser_type", length = 50)
    private String parserType;

    @Column(length = 50)
    private String status;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "priority_score")
    private Integer priorityScore = 0;

    @Column(name = "next_fetch_at")
    private LocalDateTime nextFetchAt;

    @Column(name = "fetch_interval_minutes")
    private Integer fetchIntervalMinutes = 60;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;
}
