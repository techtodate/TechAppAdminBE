package com.app.admin.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tech_updates", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class TechUpdate {
    @Id
    @SequenceGenerator(name = "tech_updates_id_generator", sequenceName = "tech_updates_id_seq", schema = "public", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tech_updates_id_generator")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull
    @Column(name = "tech_id", nullable = false)
    private Long techId;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "source_url", length = 255)
    private String sourceUrl;

    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(length = 255)
    private String source;

    @Column(name = "pulled_at")
    private LocalDateTime pulledAt;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "summary_heading", length = 255)
    private String summaryHeading;

    @Column(name = "summary_status", length = 50)
    private String summaryStatus = "PENDING";

    @Column(name = "summary_model", length = 255)
    private String summaryModel;

    @Column(name = "summary_prompt_version", length = 255)
    private String summaryPromptVersion;

    @Column(name = "summarized_at")
    private LocalDateTime summarizedAt;

    @Column(name = "summary_attempts")
    private Integer summaryAttempts = 0;

    @Column(name = "summary_error", columnDefinition = "TEXT")
    private String summaryError;

    @Column(name = "created_by")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createdBy;
}
