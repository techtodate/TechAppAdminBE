package com.app.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.admin.model.MedicalUpdate;

@Repository
public interface MedicalUpdateRepository extends JpaRepository<MedicalUpdate, Long> {
}
