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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "verification_review_history", schema = "public")
@Getter @Setter @NoArgsConstructor
public class VerificationReviewHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "verification_request_id", nullable = false) private Long verificationRequestId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "admin_user_id", nullable = false) private AdminUser adminUser;
    @Column(nullable = false, length = 50) private String action;
    @Column(name = "section_code", length = 50) private String sectionCode;
    @Column(name = "evidence_id") private Long evidenceId;
    @Column(name = "previous_status", length = 30) private String previousStatus;
    @Column(name = "new_status", length = 30) private String newStatus;
    @Column(columnDefinition = "text") private String comments;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
