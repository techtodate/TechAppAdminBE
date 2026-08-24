package com.app.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.admin.model.MedicalSubject;

@Repository
public interface MedicalSubjectRepository extends JpaRepository<MedicalSubject, Long> {
}
