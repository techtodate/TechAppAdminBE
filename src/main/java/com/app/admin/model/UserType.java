package com.app.admin.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_types", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class UserType {
    @Id
    @SequenceGenerator(name = "user_types_id_generator", sequenceName = "user_types_id_seq",
            schema = "public", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_types_id_generator")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull
    @Column(name = "field_id", nullable = false)
    private Long fieldId;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String code;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false,
            columnDefinition = "timestamp without time zone")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @Size(max = 30)
    @Column(name = "profile_type", length = 30)
    private String profileType;
}
