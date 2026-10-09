package com.app.admin.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.app.admin.model.City;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    @Query("select c from City c where c.administrativeAreaId = :districtId")
    List<City> findByDistrictId(@Param("districtId") Long districtId, Sort sort);
    Optional<City> findFirstByCountryIdAndNameIgnoreCase(Long countryId, String name);
    Optional<City> findFirstByCountryIdAndAdministrativeAreaIdAndNameIgnoreCase(Long countryId, Long administrativeAreaId, String name);
    Optional<City> findFirstByNameIgnoreCase(String name);
}
