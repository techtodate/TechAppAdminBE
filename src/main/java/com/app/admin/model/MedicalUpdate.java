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
@Table(name = "medical_updates", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class MedicalUpdate {
    @Id
    @SequenceGenerator(name = "medical_updates_id_generator", sequenceName = "medical_updates_id_seq", schema = "public", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "medical_updates_id_generator")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(length = 255)
    private String source;

    @Column(name = "pulled_at")
    private LocalDateTime pulledAt;
}
