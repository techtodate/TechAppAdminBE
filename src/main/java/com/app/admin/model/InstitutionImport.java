package com.app.admin.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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
@Table(name = "institution_imports", schema = "public")
@Getter @Setter @NoArgsConstructor
public class InstitutionImport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull
    @Column(name = "country_id", nullable = false)
    private Long countryId;

    @NotNull
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName = "pending_upload";

    @NotBlank
    @Size(max = 1000)
    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath = "pending_upload";

    @NotBlank
    @Size(max = 64)
    @Column(name = "file_checksum", nullable = false, length = 64)
    private String fileChecksum = "pending_upload";

    @Column(name = "total_records", nullable = false)
    private Integer totalRecords = 0;

    @Column(name = "new_count", nullable = false)
    private Integer newCount = 0;

    @Column(name = "update_count", nullable = false)
    private Integer updateCount = 0;

    @Column(name = "unchanged_count", nullable = false)
    private Integer unchangedCount = 0;

    @Column(name = "duplicate_count", nullable = false)
    private Integer duplicateCount = 0;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount = 0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ImportStatus status = ImportStatus.CREATED;

    @Column(name = "inserted_count", nullable = false)
    private Integer insertedCount = 0;

    @Column(name = "updated_count", nullable = false)
    private Integer updatedCount = 0;

    @Column(name = "skipped_count", nullable = false)
    private Integer skippedCount = 0;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", insertable = false, updatable = false)
    private InstitutionSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", insertable = false, updatable = false)
    private Country country;

    // Transient attributes for business logic and API compatibility
    @Transient
    private String version = "1.0";

    @Transient
    private String importType = "FULL";

    @Transient
    private LocalDateTime startedAt;

    @Transient
    private LocalDateTime completedAt;

    @Transient
    private Integer validRecords = 0;

    public Integer getValidRecords() {
        if (validRecords != null && validRecords > 0) return validRecords;
        return Math.max(0, (totalRecords != null ? totalRecords : 0) - (errorCount != null ? errorCount : 0));
    }

    public LocalDateTime getStartedAt() {
        return startedAt != null ? startedAt : createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt != null ? completedAt : appliedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
        if (this.appliedAt == null) {
            this.appliedAt = completedAt;
        }
    }

    public void setNewCount(Integer count) {
        this.newCount = count != null ? count : 0;
        this.insertedCount = this.newCount;
    }

    public void setInsertedCount(Integer count) {
        this.insertedCount = count != null ? count : 0;
        this.newCount = this.insertedCount;
    }

    public void setUpdatedCount(Integer count) {
        this.updatedCount = count != null ? count : 0;
        this.updateCount = this.updatedCount;
    }

    public void setUpdateCount(Integer count) {
        this.updateCount = count != null ? count : 0;
        this.updatedCount = this.updateCount;
    }

    public String getFileHash() {
        return fileChecksum;
    }

    public void setFileHash(String fileHash) {
        this.fileChecksum = fileHash;
    }

    public String getErrorMessage() {
        return failureReason;
    }

    public void setErrorMessage(String errorMessage) {
        this.failureReason = errorMessage;
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> map = new HashMap<>();
        map.put("total", totalRecords);
        map.put("valid", getValidRecords());
        map.put("inserted", insertedCount);
        map.put("updated", updatedCount);
        map.put("unchanged", unchangedCount);
        map.put("duplicate", duplicateCount);
        map.put("error", errorCount);
        return map;
    }

    public void setSummary(Map<String, Object> summary) {
        // backward compatibility
    }

    public LocalDateTime getUpdatedAt() {
        return appliedAt != null ? appliedAt : (completedAt != null ? completedAt : createdAt);
    }
}
