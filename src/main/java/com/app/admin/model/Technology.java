package com.app.admin.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "technologies",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(
                name = "technologies_name_key",
                columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
public class Technology {
    @Id
    @SequenceGenerator(
            name = "technologies_id_generator",
            sequenceName = "technologies_id_seq",
            schema = "public",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "technologies_id_generator")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank
    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "timestamp without time zone")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(name = "category_id", nullable = true)
    private Long categoryId;

    @Column(name = "field_id", nullable = true)
    private Long fieldId;
}
