package com.app.admin.repository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.app.admin.model.State;
@Repository public interface StateRepository extends JpaRepository<State, Long> {
    List<State> findByCountryId(Long countryId, Sort sort);
}
