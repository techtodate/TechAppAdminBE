package com.app.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.app.admin.model.TrainingSettlement;
public interface TrainingSettlementRepository extends JpaRepository<TrainingSettlement, Long>, JpaSpecificationExecutor<TrainingSettlement> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from TrainingSettlement s where s.id = :id") java.util.Optional<TrainingSettlement> findByIdForUpdate(@Param("id") Long id);
}
