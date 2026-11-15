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
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "verification_identity", schema = "public")
@Getter @Setter @NoArgsConstructor
public class VerificationIdentity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "verification_request_id", nullable = false, unique = true) private Long verificationRequestId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(length = 100) private String provider;
    @Column(name = "provider_reference", length = 255) private String providerReference;
    @Column(name = "document_type", length = 100) private String documentType;
    @Column(name = "verification_status", nullable = false, length = 30) private String verificationStatus;
    @Column(name = "name_match") private Boolean nameMatch;
    @Column(name = "document_valid") private Boolean documentValid;
    @Column(name = "liveness_verified") private Boolean livenessVerified;
    @Column(name = "date_of_birth_match") private Boolean dateOfBirthMatch;
    @Column(name = "verified_name", length = 255) private String verifiedName;
    @Column(name = "verified_at") private LocalDateTime verifiedAt;
    @Column(name = "review_status", nullable = false, length = 30) private String reviewStatus = "PENDING";
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by") private AdminUser reviewedBy;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @Column(name = "review_notes", columnDefinition = "text") private String reviewNotes;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
