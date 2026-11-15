package com.app.admin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "training_refunds", schema = "public") @Getter @Setter @NoArgsConstructor
public class TrainingRefund {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cancellation_id", nullable = false) private TrainingCancellation cancellation;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_id", nullable = false) private TrainingPayment payment;
    @Column(name = "enrollment_id", nullable = false) private Integer enrollmentId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_provider_id") private PaymentProvider paymentProvider;
    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2) private BigDecimal refundAmount;
    @Column(nullable = false, length = 10) private String currency;
    @Column(name = "provider_refund_id", length = 200) private String providerRefundId;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "requested_at", insertable = false, updatable = false) private LocalDateTime requestedAt;
    @Column(name = "processed_at") private LocalDateTime processedAt;
    @Column(name = "failure_reason", length = 1000) private String failureReason;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) private LocalDateTime updatedAt;
}
