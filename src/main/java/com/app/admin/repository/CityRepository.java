package com.app.admin.repository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.app.admin.model.City;
@Repository public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findByDistrictId(Long districtId, Sort sort);
}
