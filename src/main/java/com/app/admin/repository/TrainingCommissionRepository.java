package com.app.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.app.admin.model.TrainingCommission;
public interface TrainingCommissionRepository extends JpaRepository<TrainingCommission, Long>, JpaSpecificationExecutor<TrainingCommission> {}
