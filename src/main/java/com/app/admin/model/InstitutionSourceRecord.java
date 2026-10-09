package com.app.admin.model;

import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
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
@Table(name = "institution_source_records", schema = "public", uniqueConstraints = {
        @UniqueConstraint(name = "uq_source_record", columnNames = {"source_id", "source_identifier"})
})
@Getter @Setter @NoArgsConstructor
public class InstitutionSourceRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull
    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @NotNull
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @NotBlank
    @Size(max = 250)
    @Column(name = "source_identifier", nullable = false, length = 250)
    private String sourceIdentifier;

    @Size(max = 500)
    @Column(name = "source_name", length = 500)
    private String sourceName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_data", columnDefinition = "jsonb")
    private Map<String, Object> sourceData;

    @CreationTimestamp
    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private LocalDateTime firstSeenAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "last_import_id")
    private Long lastImportId;

    @NotNull
    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", insertable = false, updatable = false)
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", insertable = false, updatable = false)
    private InstitutionSource source;

    public Map<String, Object> getRawData() {
        return sourceData;
    }

    public void setRawData(Map<String, Object> rawData) {
        this.sourceData = rawData;
    }
}
