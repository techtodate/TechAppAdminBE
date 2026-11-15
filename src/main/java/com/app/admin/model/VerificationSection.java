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
@Table(name = "verification_sections", schema = "public", uniqueConstraints =
        @UniqueConstraint(name = "uk_verification_section_request_code", columnNames = {"verification_request_id", "section_code"}))
@Getter @Setter @NoArgsConstructor
public class VerificationSection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "verification_request_id", nullable = false) private Long verificationRequestId;
    @Column(name = "section_code", nullable = false, length = 50) private String sectionCode;
    @Column(name = "user_status", nullable = false, length = 30) private String userStatus = "NOT_STARTED";
    @Column(name = "review_status", nullable = false, length = 30) private String reviewStatus = "PENDING";
    @Column(name = "review_notes", columnDefinition = "text") private String reviewNotes;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by") private AdminUser reviewedBy;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
