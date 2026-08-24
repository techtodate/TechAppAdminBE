package com.app.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.admin.model.MedicalCategory;

@Repository
public interface MedicalCategoryRepository extends JpaRepository<MedicalCategory, Long> {
}
