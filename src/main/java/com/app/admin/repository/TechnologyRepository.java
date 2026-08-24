package com.app.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.admin.model.Technology;

@Repository
public interface TechnologyRepository extends JpaRepository<Technology, Long> {
}
