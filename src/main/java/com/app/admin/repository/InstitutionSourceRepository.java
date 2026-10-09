package com.app.admin.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.app.admin.model.InstitutionSource;

@Repository
public interface InstitutionSourceRepository extends JpaRepository<InstitutionSource, Long> {
    List<InstitutionSource> findByCountryId(Long countryId, Sort sort);
    List<InstitutionSource> findByCountryId(Long countryId);
    @Query("select s from InstitutionSource s where s.countryId = :countryId and lower(s.sourceCode) = lower(:code)")
    Optional<InstitutionSource> findByCountryIdAndCodeIgnoreCase(@Param("countryId") Long countryId, @Param("code") String code);
    @Query("select s from InstitutionSource s where lower(s.sourceCode) = lower(:code)")
    Optional<InstitutionSource> findByCodeIgnoreCase(@Param("code") String code);
}
