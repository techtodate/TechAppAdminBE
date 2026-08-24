package com.app.admin.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fields", schema = "public",
        uniqueConstraints = @UniqueConstraint(name = "fields_code_key", columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
public class Field {
    @Id
    @SequenceGenerator(name = "fields_id_generator", sequenceName = "fields_id_seq", schema = "public", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fields_id_generator")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 255)
    private String code;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @NotNull
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime updatedAt;

    @Column(name = "icon_name", length = 255)
    private String iconName;
}
