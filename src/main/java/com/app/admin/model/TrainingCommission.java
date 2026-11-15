package com.app.admin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "training_commissions", schema = "public") @Getter @Setter @NoArgsConstructor
public class TrainingCommission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "enrollment_id", nullable = false) private Integer enrollmentId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_id", nullable = false) private TrainingPayment payment;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "commission_rule_id") private PlatformCommissionRule commissionRule;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "commission_rule_version_id") private PlatformCommissionRuleVersion commissionRuleVersion;
    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2) private BigDecimal grossAmount;
    @Column(name = "commission_percentage", precision = 7, scale = 4) private BigDecimal commissionPercentage;
    @Column(name = "commission_fixed_amount", precision = 12, scale = 2) private BigDecimal commissionFixedAmount;
    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2) private BigDecimal commissionAmount;
    @Column(name = "trainer_amount", nullable = false, precision = 12, scale = 2) private BigDecimal trainerAmount;
    @Column(nullable = false, length = 10) private String currency;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "calculated_at") private LocalDateTime calculatedAt;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) private LocalDateTime updatedAt;
}
