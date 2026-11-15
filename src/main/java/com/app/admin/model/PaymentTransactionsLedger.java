package com.app.admin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "payment_transactions_ledger", schema = "public") @Getter @Setter @NoArgsConstructor
public class PaymentTransactionsLedger {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "enrollment_id") private Integer enrollmentId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_id") private TrainingPayment payment;
    @Column(name = "refund_id") private Long refundId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "commission_id") private TrainingCommission commission;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "settlement_id") private TrainingSettlement settlement;
    @Column(name = "transaction_type", nullable = false, length = 50) private String transactionType;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 10) private String currency;
    @Column(name = "reference_id", length = 200) private String referenceId;
    @Column(length = 1000) private String description;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
}
