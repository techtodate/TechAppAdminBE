package com.app.admin.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.District;
import com.app.admin.repository.DistrictRepository;
import com.app.admin.repository.StateRepository;

@Service
public class DistrictService {
    private final DistrictRepository repository; private final StateRepository stateRepository;
    public DistrictService(DistrictRepository repository, StateRepository stateRepository) { this.repository = repository; this.stateRepository = stateRepository; }
    @Transactional(readOnly = true) public List<District> findAll(Long stateId) { return stateId == null ? repository.findAll(sort()) : repository.findByStateId(stateId, sort()); }
    @Transactional(readOnly = true) public District findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("District", id)); }
    @Transactional public District create(District value) { validateState(value.getStateId()); value.setId(null); value.setCreatedAt(null); value.setUpdatedAt(null); return repository.saveAndFlush(value); }
    @Transactional public District update(Long id, District value) { validateState(value.getStateId()); District current = findById(id); current.setStateId(value.getStateId()); current.setCode(value.getCode()); current.setName(value.getName()); current.setDisplayOrder(value.getDisplayOrder()); current.setActive(value.getActive()); return repository.saveAndFlush(current); }
    @Transactional public void delete(Long id) { repository.delete(findById(id)); repository.flush(); }
    private void validateState(Long id) { if (!stateRepository.existsById(id)) throw new ResourceNotFoundException("State", id); }
    private Sort sort() { return Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("name")); }
}
