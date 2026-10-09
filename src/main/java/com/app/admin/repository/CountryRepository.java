package com.app.admin.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.app.admin.model.Country;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {
    @Query("select c from Country c where lower(c.isoCode) = lower(:code) or lower(c.iso3Code) = lower(:code)")
    Optional<Country> findByCodeIgnoreCase(@Param("code") String code);
    Optional<Country> findByNameIgnoreCase(String name);
}
