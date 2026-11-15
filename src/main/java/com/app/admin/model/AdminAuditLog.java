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
@Table(name = "admin_audit_logs", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class AdminAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private AdminUser adminUser;
    @Column(nullable = false, length = 150) private String action;
    @Column(length = 100) private String module;
    @Column(name = "entity_type", length = 100) private String entityType;
    @Column(name = "entity_id", length = 100) private String entityId;
    @Column(columnDefinition = "text") private String description;
    @Column(name = "ip_address", length = 100) private String ipAddress;
    @Column(name = "user_agent", columnDefinition = "text") private String userAgent;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
}
