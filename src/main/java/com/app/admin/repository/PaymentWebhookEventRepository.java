package com.app.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.app.admin.model.PaymentWebhookEvent;
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long>, JpaSpecificationExecutor<PaymentWebhookEvent> {}
