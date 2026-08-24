package com.app.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.admin.model.MedicalSource;

@Repository
public interface MedicalSourceRepository extends JpaRepository<MedicalSource, Long> {
}
