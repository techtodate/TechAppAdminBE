package com.app.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.app.admin.model.TrainingRefund;
import java.util.Collection;
public interface TrainingRefundRepository extends JpaRepository<TrainingRefund, Long>, JpaSpecificationExecutor<TrainingRefund> {
    boolean existsByPayment_IdAndStatusIn(Long paymentId, Collection<String> statuses);
}
