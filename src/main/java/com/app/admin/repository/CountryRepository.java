package com.app.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.app.admin.model.Country;
@Repository public interface CountryRepository extends JpaRepository<Country, Long> {}
