package com.app.admin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "training_settlement_items", schema = "public", uniqueConstraints = @UniqueConstraint(name = "uq_training_settlement_commission", columnNames = {"settlement_id", "commission_id"})) @Getter @Setter @NoArgsConstructor
public class TrainingSettlementItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "settlement_id", nullable = false) private TrainingSettlement settlement;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "commission_id", nullable = false) private TrainingCommission commission;
    @Column(name = "enrollment_id", nullable = false) private Integer enrollmentId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
}
