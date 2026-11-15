package com.app.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.app.admin.model.TrainingCancellation;
public interface TrainingCancellationRepository extends JpaRepository<TrainingCancellation, Long>, JpaSpecificationExecutor<TrainingCancellation> {}
