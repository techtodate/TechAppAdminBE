package com.app.admin.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.app.admin.model.InstitutionImportRecord;
import com.app.admin.model.RecordClassification;

@Repository
public interface InstitutionImportRecordRepository extends JpaRepository<InstitutionImportRecord, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<InstitutionImportRecord> {
    Page<InstitutionImportRecord> findByImportId(Long importId, Pageable pageable);
    Page<InstitutionImportRecord> findByImportIdAndAction(Long importId, String action, Pageable pageable);
    default Page<InstitutionImportRecord> findByImportIdAndClassification(Long id, RecordClassification classification, Pageable pageable) { return findByImportIdAndAction(id, classification.name(), pageable); }
    List<InstitutionImportRecord> findByImportIdOrderByRowNumberAsc(Long importId);
    default List<InstitutionImportRecord> findByImportIdOrderByRowIndexAsc(Long id) { return findByImportIdOrderByRowNumberAsc(id); }
    long countByImportIdAndAction(Long importId, String action);
    default long countByImportIdAndClassification(Long id, RecordClassification classification) { return countByImportIdAndAction(id, classification.name()); }
    void deleteByImportId(Long importId);
}
