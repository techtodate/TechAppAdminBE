package com.app.admin.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.Country;
import com.app.admin.repository.CountryRepository;

@Service
public class CountryService {
    private final CountryRepository repository;
    public CountryService(CountryRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<Country> findAll() { return repository.findAll(masterSort()); }
    @Transactional(readOnly = true) public Country findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Country", id)); }
    @Transactional public Country create(Country value) { value.setId(null); value.setCreatedAt(null); value.setUpdatedAt(null); return repository.saveAndFlush(value); }
    @Transactional public Country update(Long id, Country value) {
        Country current = findById(id); current.setCode(value.getCode()); current.setName(value.getName());
        current.setPhoneCode(value.getPhoneCode()); current.setActive(value.getActive());
        return repository.saveAndFlush(current);
    }
    @Transactional public void delete(Long id) { repository.delete(findById(id)); repository.flush(); }
    private Sort masterSort() { return Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")); }
}
