package com.app.admin.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.app.admin.model.AdministrativeArea;

@Repository
public interface AdministrativeAreaRepository extends JpaRepository<AdministrativeArea, Long> {
    List<AdministrativeArea> findByCountryIdAndActiveTrue(Long countryId);
    Optional<AdministrativeArea> findByCountryIdAndNameIgnoreCase(Long countryId, String name);
    Optional<AdministrativeArea> findByCountryIdAndCodeIgnoreCase(Long countryId, String code);
}
