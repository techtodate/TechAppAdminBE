package com.app.admin.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.State;
import com.app.admin.repository.CountryRepository;
import com.app.admin.repository.StateRepository;

@Service
public class StateService {
    private final StateRepository repository; private final CountryRepository countryRepository;
    public StateService(StateRepository repository, CountryRepository countryRepository) { this.repository = repository; this.countryRepository = countryRepository; }
    @Transactional(readOnly = true) public List<State> findAll(Long countryId) { return countryId == null ? repository.findAll(sort()) : repository.findByCountryId(countryId, sort()); }
    @Transactional(readOnly = true) public State findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("State", id)); }
    @Transactional public State create(State value) { validateCountry(value.getCountryId()); value.setId(null); value.setCreatedAt(null); value.setUpdatedAt(null); return repository.saveAndFlush(value); }
    @Transactional public State update(Long id, State value) { validateCountry(value.getCountryId()); State current = findById(id); current.setCountryId(value.getCountryId()); current.setCode(value.getCode()); current.setName(value.getName()); current.setDisplayOrder(value.getDisplayOrder()); current.setActive(value.getActive()); return repository.saveAndFlush(current); }
    @Transactional public void delete(Long id) { repository.delete(findById(id)); repository.flush(); }
    private void validateCountry(Long id) { if (!countryRepository.existsById(id)) throw new ResourceNotFoundException("Country", id); }
    private Sort sort() { return Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("name")); }
}
