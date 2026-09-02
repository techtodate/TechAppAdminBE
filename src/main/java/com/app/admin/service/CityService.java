package com.app.admin.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.City;
import com.app.admin.repository.CityRepository;
import com.app.admin.repository.DistrictRepository;

@Service
public class CityService {
    private final CityRepository repository; private final DistrictRepository districtRepository;
    public CityService(CityRepository repository, DistrictRepository districtRepository) { this.repository = repository; this.districtRepository = districtRepository; }
    @Transactional(readOnly = true) public List<City> findAll(Long districtId) { return districtId == null ? repository.findAll(sort()) : repository.findByDistrictId(districtId, sort()); }
    @Transactional(readOnly = true) public City findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("City", id)); }
    @Transactional public City create(City value) { validateDistrict(value.getDistrictId()); value.setId(null); value.setCreatedAt(null); value.setUpdatedAt(null); return repository.saveAndFlush(value); }
    @Transactional public City update(Long id, City value) { validateDistrict(value.getDistrictId()); City current = findById(id); current.setDistrictId(value.getDistrictId()); current.setCode(value.getCode()); current.setName(value.getName()); current.setPostalCode(value.getPostalCode()); current.setDisplayOrder(value.getDisplayOrder()); current.setActive(value.getActive()); return repository.saveAndFlush(current); }
    @Transactional public void delete(Long id) { repository.delete(findById(id)); repository.flush(); }
    private void validateDistrict(Long id) { if (!districtRepository.existsById(id)) throw new ResourceNotFoundException("District", id); }
    private Sort sort() { return Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("name")); }
}
