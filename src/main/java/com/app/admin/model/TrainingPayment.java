package com.app.admin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "training_payments", schema = "public") @Getter @Setter @NoArgsConstructor
public class TrainingPayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_order_id", nullable = false) private TrainingPaymentOrder paymentOrder;
    @Column(name = "enrollment_id", nullable = false) private Integer enrollmentId;
    @Column(name = "trainee_user_id", nullable = false) private Long traineeUserId;
    @Column(name = "trainer_user_id", nullable = false) private Long trainerUserId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_provider_id") private PaymentProvider paymentProvider;
    @Column(name = "provider_payment_id", length = 200) private String providerPaymentId;
    @Column(name = "provider_order_id", length = 200) private String providerOrderId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 10) private String currency;
    @Column(name = "payment_method", length = 50) private String paymentMethod;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "captured_at") private LocalDateTime capturedAt;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) private LocalDateTime updatedAt;
}
