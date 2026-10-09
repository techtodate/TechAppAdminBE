package com.app.admin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@Table(name = "institution_import_record_matches", schema = "public")
@Getter @Setter @NoArgsConstructor
public class InstitutionImportRecordMatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull
    @Column(name = "import_record_id", nullable = false)
    private Long importRecordId;

    @NotNull
    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @NotNull
    @Column(name = "match_score", nullable = false)
    private Integer matchScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "match_reason", columnDefinition = "jsonb")
    private Map<String, Object> matchReason;

    @NotNull
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String resolution = "PENDING";

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_record_id", insertable = false, updatable = false)
    private InstitutionImportRecord importRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", insertable = false, updatable = false)
    private Institution institution;

    public Long getMatchedInstitutionId() {
        return institutionId;
    }

    public void setMatchedInstitutionId(Long matchedInstitutionId) {
        this.institutionId = matchedInstitutionId;
    }

    public Institution getMatchedInstitution() {
        return institution;
    }

    public void setMatchedInstitution(Institution inst) {
        this.institution = inst;
    }

    public ResolutionStatus getResolutionStatus() {
        if (resolution == null) return ResolutionStatus.PENDING;
        try {
            return ResolutionStatus.valueOf(resolution);
        } catch (Exception e) {
            return ResolutionStatus.PENDING;
        }
    }

    public void setResolutionStatus(ResolutionStatus status) {
        this.resolution = status != null ? status.name() : "PENDING";
    }

    public MatchConfidence getMatchConfidence() {
        if (matchScore == null) return MatchConfidence.POSSIBLE_DUPLICATE;
        return matchScore.doubleValue() >= 90.0 ? MatchConfidence.HIGH_CONFIDENCE : MatchConfidence.POSSIBLE_DUPLICATE;
    }

    public void setMatchConfidence(MatchConfidence confidence) {
        // derived from matchScore
    }
}
