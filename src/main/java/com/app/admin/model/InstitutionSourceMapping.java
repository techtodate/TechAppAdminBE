package com.app.admin.model;

import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "institution_source_mappings", schema = "public", uniqueConstraints = {
        @UniqueConstraint(name = "uq_source_mapping", columnNames = {"source_id", "source_field_name"})
})
@Getter @Setter @NoArgsConstructor
public class InstitutionSourceMapping {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @NotBlank
    @Size(max = 200)
    @Column(name = "source_field_name", nullable = false, length = 200)
    private String sourceFieldName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "standard_field_code", nullable = false, length = 100)
    private String standardFieldCode;

    @Size(max = 100)
    @Column(name = "transformation_rule", length = 100)
    private String transformationRule;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transformation_config", columnDefinition = "jsonb")
    private Map<String, Object> transformationConfig;

    @NotNull
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @NotNull
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", insertable = false, updatable = false)
    private InstitutionSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_field_code", referencedColumnName = "field_code", insertable = false, updatable = false)
    private InstitutionStandardField standardField;

    public Long getStandardFieldId() {
        return standardField != null ? standardField.getId() : null;
    }

    public void setStandardFieldId(Long standardFieldId) {
        // backward-compatibility
    }

    public String getDefaultValue() {
        if (transformationConfig != null && transformationConfig.containsKey("default_value")) {
            return String.valueOf(transformationConfig.get("default_value"));
        }
        return null;
    }

    public void setDefaultValue(String defaultValue) {
        if (defaultValue != null) {
            if (transformationConfig == null) {
                transformationConfig = new java.util.HashMap<>();
            }
            transformationConfig.put("default_value", defaultValue);
        }
    }
}

