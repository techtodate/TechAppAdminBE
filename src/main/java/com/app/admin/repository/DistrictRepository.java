package com.app.admin.repository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.app.admin.model.District;
@Repository public interface DistrictRepository extends JpaRepository<District, Long> {
    List<District> findByStateId(Long stateId, Sort sort);
}
