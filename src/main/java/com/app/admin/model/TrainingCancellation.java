package com.app.admin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "training_cancellations", schema = "public") @Getter @Setter @NoArgsConstructor
public class TrainingCancellation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "enrollment_id", nullable = false) private Integer enrollmentId;
    @Column(name = "requested_by_user_id", nullable = false) private Long requestedByUserId;
    @Column(name = "cancellation_stage", nullable = false, length = 40) private String cancellationStage;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "policy_rule_id") private RefundPolicyRule policyRule;
    @Column(name = "refund_percentage", precision = 5, scale = 2) private BigDecimal refundPercentage;
    @Column(name = "payment_amount", precision = 12, scale = 2) private BigDecimal paymentAmount;
    @Column(name = "expected_refund_amount", precision = 12, scale = 2) private BigDecimal expectedRefundAmount;
    @Column(length = 500) private String reason;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "requested_at", insertable = false, updatable = false) private LocalDateTime requestedAt;
    @Column(name = "approved_at") private LocalDateTime approvedAt;
    @Column(name = "rejected_at") private LocalDateTime rejectedAt;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) private LocalDateTime updatedAt;
}
