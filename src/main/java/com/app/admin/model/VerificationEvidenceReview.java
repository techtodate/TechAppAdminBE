package com.app.admin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "verification_evidence_reviews", schema = "public",
        uniqueConstraints = @UniqueConstraint(name = "uk_verification_evidence_review_source", columnNames = "source_evidence_id"))
@Getter @Setter @NoArgsConstructor
public class VerificationEvidenceReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "verification_request_id", nullable = false) private Long verificationRequestId;
    @Column(name = "source_evidence_id", nullable = false) private Long sourceEvidenceId;
    @Column(name = "review_status", nullable = false, length = 30) private String reviewStatus = "PENDING";
    @Column(name = "review_notes", columnDefinition = "text") private String reviewNotes;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by") private AdminUser reviewedBy;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
