package com.app.admin.model;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "payment_webhook_events", schema = "public") @Getter @Setter @NoArgsConstructor
public class PaymentWebhookEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_provider_id") private PaymentProvider paymentProvider;
    @Column(name = "provider_event_id", nullable = false, length = 200) private String providerEventId;
    @Column(name = "event_type", nullable = false, length = 150) private String eventType;
    @JsonIgnore @Column(columnDefinition = "text") private String payload;
    @Column(name = "processing_status", nullable = false, length = 30) private String processingStatus;
    @Column(name = "received_at", insertable = false, updatable = false) private LocalDateTime receivedAt;
    @Column(name = "processed_at") private LocalDateTime processedAt;
    @Column(name = "failure_reason", length = 1000) private String failureReason;
}
