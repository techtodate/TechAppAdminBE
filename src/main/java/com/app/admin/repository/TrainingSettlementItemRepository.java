package com.app.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Pageable;
import java.util.List;
import com.app.admin.model.TrainingSettlementItem;
public interface TrainingSettlementItemRepository extends JpaRepository<TrainingSettlementItem, Long>, JpaSpecificationExecutor<TrainingSettlementItem> {
    List<TrainingSettlementItem> findBySettlement_Id(Long settlementId, Pageable pageable);
}
