package com.app.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.admin.model.ProfileType;

@Repository
public interface ProfileTypeRepository extends JpaRepository<ProfileType, Long> {
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
