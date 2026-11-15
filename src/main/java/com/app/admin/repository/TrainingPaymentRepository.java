package com.app.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.app.admin.model.TrainingPayment;
public interface TrainingPaymentRepository extends JpaRepository<TrainingPayment, Long>, JpaSpecificationExecutor<TrainingPayment> {}
