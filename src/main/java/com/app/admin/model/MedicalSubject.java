package com.app.admin.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medical_subjects", schema = "public",
        uniqueConstraints = @UniqueConstraint(name = "medical_subjects_name_key", columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
public class MedicalSubject {
    @Id
    @SequenceGenerator(name = "medical_subjects_id_generator", sequenceName = "medical_subjects_id_seq", schema = "public", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "medical_subjects_id_generator")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime updatedAt;
}
