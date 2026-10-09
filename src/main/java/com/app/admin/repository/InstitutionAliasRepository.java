package com.app.admin.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.app.admin.model.InstitutionAlias;

@Repository
public interface InstitutionAliasRepository extends JpaRepository<InstitutionAlias, Long> {
    List<InstitutionAlias> findByInstitutionId(Long institutionId);
    List<InstitutionAlias> findByNormalizedAlias(String normalizedAlias);
}
