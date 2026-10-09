package com.app.admin.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.app.admin.model.InstitutionImport;

@Repository
public interface InstitutionImportRepository extends JpaRepository<InstitutionImport, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<InstitutionImport> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from InstitutionImport i where i.id = :id")
    java.util.Optional<InstitutionImport> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
    Page<InstitutionImport> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<InstitutionImport> findBySourceIdOrderByCreatedAtDesc(Long sourceId);
}
