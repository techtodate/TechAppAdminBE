package com.app.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.admin.model.TechSource;

@Repository
public interface TechSourceRepository extends JpaRepository<TechSource, Long> {
}
