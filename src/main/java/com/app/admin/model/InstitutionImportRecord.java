package com.app.admin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "institution_import_records", schema = "public")
@Getter @Setter @NoArgsConstructor
public class InstitutionImportRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull
    @Column(name = "import_id", nullable = false)
    private Long importId;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Size(max = 250)
    @Column(name = "source_identifier", length = 250)
    private String sourceIdentifier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "normalized_data", columnDefinition = "jsonb")
    private Map<String, Object> normalizedData;

    @Size(max = 50)
    @Column(length = 50)
    private String action;

    @NotNull
    @Size(max = 50)
    @Column(name = "processing_status", nullable = false, length = 50)
    private String processingStatus = "PENDING";

    @Column(name = "institution_id")
    private Long institutionId;

    @Column(name = "match_confidence", precision = 5, scale = 2)
    private BigDecimal matchConfidence;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id", insertable = false, updatable = false)
    private InstitutionImport institutionImport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", insertable = false, updatable = false)
    private Institution institution;

    public Integer getRowIndex() {
        return rowNumber;
    }

    public void setRowIndex(Integer rowIndex) {
        this.rowNumber = rowIndex;
    }

    public Map<String, Object> getMappedData() {
        return normalizedData;
    }

    public void setMappedData(Map<String, Object> mappedData) {
        this.normalizedData = mappedData;
    }

    public RecordClassification getClassification() {
        if (action == null) return null;
        try {
            return RecordClassification.valueOf(action);
        } catch (Exception e) {
            return null;
        }
    }

    public void setClassification(RecordClassification classification) {
        this.action = classification != null ? classification.name() : null;
    }

    public ValidationStatus getValidationStatus() {
        if (processingStatus == null) return ValidationStatus.VALID;
        try {
            return ValidationStatus.valueOf(processingStatus);
        } catch (Exception e) {
            return ValidationStatus.VALID;
        }
    }

    public void setValidationStatus(ValidationStatus status) {
        this.processingStatus = status != null ? status.name() : "PENDING";
    }

    public Long getTargetInstitutionId() {
        return institutionId;
    }

    public void setTargetInstitutionId(Long targetInstitutionId) {
        this.institutionId = targetInstitutionId;
    }

    public List<String> getValidationErrors() {
        if (errorMessage == null || errorMessage.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(errorMessage.split("; "));
    }

    public void setValidationErrors(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            this.errorMessage = null;
        } else {
            this.errorMessage = String.join("; ", errors);
        }
    }
}
