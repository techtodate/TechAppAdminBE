package com.app.admin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "training_financial_admin_audit", schema = "public")
@Getter @Setter @NoArgsConstructor
public class TrainingFinancialAdminAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "admin_username", nullable = false, length = 200) private String adminUsername;
    @Column(nullable = false, length = 100) private String action;
    @Column(name = "target_type", nullable = false, length = 100) private String targetType;
    @Column(name = "target_id", nullable = false) private Long targetId;
    @Column(nullable = false, length = 30) private String result;
    @Column(columnDefinition = "text") private String details;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
}
