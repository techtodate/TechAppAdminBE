package com.app.admin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "training_payment_orders", schema = "public") @Getter @Setter @NoArgsConstructor
public class TrainingPaymentOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "enrollment_id", nullable = false) private Integer enrollmentId;
    @Column(name = "trainee_user_id", nullable = false) private Long traineeUserId;
    @Column(name = "trainer_user_id", nullable = false) private Long trainerUserId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 10) private String currency;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_provider_id") private PaymentProvider paymentProvider;
    @Column(name = "provider_order_id", length = 200) private String providerOrderId;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "expires_at") private LocalDateTime expiresAt;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) private LocalDateTime updatedAt;
}
