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
@Table(name = "categories", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class Category {
    @Id
    @SequenceGenerator(name = "categories_id_generator", sequenceName = "categories_id_seq", schema = "public", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "categories_id_generator")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    @NotNull
    @Column(name = "field_id", nullable = false)
    private Long fieldId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "has_children")
    private Boolean hasChildren = false;

    @NotNull
    @Column(nullable = false)
    private Integer level;

    @Column(name = "display_tag", length = 255)
    private String displayTag;

    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;
}
