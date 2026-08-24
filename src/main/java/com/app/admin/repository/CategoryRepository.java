package com.app.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.admin.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
