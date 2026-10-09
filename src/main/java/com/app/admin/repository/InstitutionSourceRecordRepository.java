package com.app.admin.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.app.admin.model.InstitutionSourceRecord;

@Repository
public interface InstitutionSourceRecordRepository extends JpaRepository<InstitutionSourceRecord, Long> {
    Optional<InstitutionSourceRecord> findBySourceIdAndSourceIdentifier(Long sourceId, String sourceIdentifier);
    List<InstitutionSourceRecord> findByInstitutionId(Long institutionId);
}
