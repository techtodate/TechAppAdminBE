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
@Table(name = "medical_categories", schema = "public",
        uniqueConstraints = @UniqueConstraint(name = "medical_categories_name_key", columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
public class MedicalCategory {
    @Id
    @SequenceGenerator(name = "medical_categories_id_generator", sequenceName = "medical_categories_id_seq", schema = "public", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "medical_categories_id_generator")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
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
