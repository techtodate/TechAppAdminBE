package com.app.admin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Minimal Admin-side mapping of the canonical Main App verification request. */
@Entity
@Table(name = "user_verification_requests", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class UserVerificationRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "verification_type") private String verificationType;
    @Column(nullable = false) private String status;
    // Main App persists the workflow step as a numeric index.
    @Column(name = "current_step") private Integer currentStep;
    @Column(name = "progress_percentage") private Integer progressPercentage;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @Column(name = "verified_at") private LocalDateTime verifiedAt;
    @Column(name = "expires_at") private LocalDateTime expiresAt;
    @Column(name = "changes_requested_at") private LocalDateTime changesRequestedAt;
    @Column(name = "admin_notes", columnDefinition = "text") private String adminNotes;
    @Column(name = "rejection_reason", columnDefinition = "text") private String rejectionReason;
    @Column(name = "assigned_reviewer_id") private Long assignedReviewerId;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
