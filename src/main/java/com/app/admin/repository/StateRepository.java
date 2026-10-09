package com.app.admin.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.app.admin.model.State;

@Repository
public interface StateRepository extends JpaRepository<State, Long> {
    List<State> findByCountryId(Long countryId, Sort sort);
    Optional<State> findByCountryIdAndCodeIgnoreCase(Long countryId, String code);
    Optional<State> findByCountryIdAndNameIgnoreCase(Long countryId, String name);
}
