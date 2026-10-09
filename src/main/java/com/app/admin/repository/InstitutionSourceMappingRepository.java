package com.app.admin.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.app.admin.model.InstitutionSourceMapping;

@Repository
public interface InstitutionSourceMappingRepository extends JpaRepository<InstitutionSourceMapping, Long> {
    List<InstitutionSourceMapping> findBySourceId(Long sourceId);

    @Query("SELECT m FROM InstitutionSourceMapping m WHERE m.sourceId = :sourceId AND m.standardFieldCode = :fieldCode")
    Optional<InstitutionSourceMapping> findBySourceIdAndFieldCode(@Param("sourceId") Long sourceId, @Param("fieldCode") String fieldCode);
}
