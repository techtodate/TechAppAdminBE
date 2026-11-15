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
@Table(name = "verification_evidence", schema = "public")
@Getter @Setter @NoArgsConstructor
public class VerificationEvidence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "verification_request_id", nullable = false) private Long verificationRequestId;
    @Column(name = "section_code", nullable = false, length = 50) private String sectionCode;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "document_name", nullable = false, length = 255) private String documentName;
    @Column(name = "document_type", length = 100) private String documentType;
    @Column(name = "storage_provider", length = 50) private String storageProvider;
    @Column(name = "storage_container", length = 255) private String storageContainer;
    @Column(name = "storage_key", nullable = false, length = 1000) private String storageKey;
    @Column(name = "content_type", length = 150) private String contentType;
    @Column(name = "file_size") private Long fileSize;
    @Column(name = "upload_status", length = 30) private String uploadStatus;
    @Column(name = "review_status", nullable = false, length = 30) private String reviewStatus = "PENDING";
    @Column(name = "review_notes", columnDefinition = "text") private String reviewNotes;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by") private AdminUser reviewedBy;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @Column(name = "uploaded_at") private LocalDateTime uploadedAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private LocalDateTime updatedAt;
}
