package com.app.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.app.admin.model.PaymentProvider;
public interface PaymentProviderRepository extends JpaRepository<PaymentProvider, Long> {}
