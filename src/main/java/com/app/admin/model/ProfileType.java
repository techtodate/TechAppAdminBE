package com.app.admin.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "profile_type",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_profile_master_code",
                columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
public class ProfileType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false,
            columnDefinition = "timestamp without time zone")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "timestamp without time zone")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime updatedAt;
}
