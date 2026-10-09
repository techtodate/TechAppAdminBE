package com.app.admin.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.app.admin.model.InstitutionStandardField;

@Repository
public interface InstitutionStandardFieldRepository extends JpaRepository<InstitutionStandardField, Long> {
    @Query("select f from InstitutionStandardField f where f.fieldCode = :code")
    Optional<InstitutionStandardField> findByCode(@Param("code") String code);
}
