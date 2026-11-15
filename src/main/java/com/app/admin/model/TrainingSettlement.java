package com.app.admin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "training_settlements", schema = "public") @Getter @Setter @NoArgsConstructor
public class TrainingSettlement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "trainer_user_id", nullable = false) private Long trainerUserId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_account_id") private TrainerPayoutAccount payoutAccount;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 10) private String currency;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_provider_id") private PaymentProvider paymentProvider;
    @Column(name = "provider_transfer_id", length = 200) private String providerTransferId;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "eligible_at") private LocalDateTime eligibleAt;
    @Column(name = "initiated_at") private LocalDateTime initiatedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "failure_reason", length = 1000) private String failureReason;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) private LocalDateTime updatedAt;
}
