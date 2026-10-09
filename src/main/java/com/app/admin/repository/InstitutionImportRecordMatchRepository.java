package com.app.admin.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.app.admin.model.InstitutionImportRecordMatch;

@Repository
public interface InstitutionImportRecordMatchRepository extends JpaRepository<InstitutionImportRecordMatch, Long> {
    List<InstitutionImportRecordMatch> findByImportRecordId(Long importRecordId);

    @Query("SELECT m FROM InstitutionImportRecordMatch m JOIN InstitutionImportRecord r ON m.importRecordId = r.id WHERE r.importId = :importId")
    List<InstitutionImportRecordMatch> findByImportId(@Param("importId") Long importId);
}
