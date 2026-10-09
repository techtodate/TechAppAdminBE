package com.app.admin.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.app.admin.model.Institution;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Institution> {
    List<Institution> findByCountryIdAndNormalizedNameAndCityId(Long countryId, String normalizedName, Long cityId);

    @Query("select i from Institution i where i.countryId = :countryId and i.normalizedName = :normalizedName and i.administrativeAreaId = :stateId")
    List<Institution> findByCountryIdAndNormalizedNameAndStateId(@Param("countryId") Long countryId, @Param("normalizedName") String normalizedName, @Param("stateId") Long stateId);

    @Query("select distinct i.institutionType from Institution i where i.institutionType is not null order by i.institutionType")
    List<String> findInstitutionTypes();

    List<Institution> findByCountryIdAndNormalizedName(Long countryId, String normalizedName);

    List<Institution> findByCountryId(Long countryId);

    Page<Institution> findByCountryId(Long countryId, Pageable pageable);

    @Query("SELECT i FROM Institution i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(i.shortName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Institution> searchByNameOrShortName(@Param("query") String query, Pageable pageable);

    @Query("SELECT i FROM Institution i WHERE i.countryId = :countryId AND (LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(i.shortName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Institution> searchByCountryAndNameOrShortName(@Param("countryId") Long countryId, @Param("query") String query, Pageable pageable);
}
