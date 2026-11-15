package com.app.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.app.admin.model.PaymentTransactionsLedger;
public interface PaymentTransactionsLedgerRepository extends JpaRepository<PaymentTransactionsLedger, Long>, JpaSpecificationExecutor<PaymentTransactionsLedger> {}
