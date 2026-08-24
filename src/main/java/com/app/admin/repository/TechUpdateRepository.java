package com.app.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.admin.model.TechUpdate;

@Repository
public interface TechUpdateRepository extends JpaRepository<TechUpdate, Long> {
}
