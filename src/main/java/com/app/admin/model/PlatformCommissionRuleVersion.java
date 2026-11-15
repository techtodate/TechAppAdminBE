package com.app.admin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "platform_commission_rule_versions", schema = "public", uniqueConstraints = @UniqueConstraint(name = "uq_commission_rule_version", columnNames = {"commission_rule_id", "version_number"})) @Getter @Setter @NoArgsConstructor
public class PlatformCommissionRuleVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "commission_rule_id", nullable = false) private PlatformCommissionRule commissionRule;
    @Column(name = "version_number", nullable = false) private Integer versionNumber;
    @Column(precision = 7, scale = 4) private BigDecimal percentage;
    @Column(name = "fixed_amount", precision = 12, scale = 2) private BigDecimal fixedAmount;
    @Column(name = "effective_from", nullable = false) private LocalDateTime effectiveFrom;
    @Column(name = "effective_to") private LocalDateTime effectiveTo;
    @Column(nullable = false) private Boolean active = true;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
}
