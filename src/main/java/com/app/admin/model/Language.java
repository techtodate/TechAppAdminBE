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
@Table(name = "language", schema = "public", uniqueConstraints = {
        @UniqueConstraint(name = "uk_language_code", columnNames = "code"),
        @UniqueConstraint(name = "uk_language_name", columnNames = "name")})
@Getter @Setter @NoArgsConstructor
public class Language {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank @Size(max = 10) @Column(nullable = false, length = 10)
    private String code;

    @NotBlank @Size(max = 100) @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 100) @Column(name = "native_name", length = 100)
    private String nativeName;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @NotNull @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @UpdateTimestamp @Column(name = "updated_at")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime updatedAt;
}
