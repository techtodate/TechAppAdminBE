package com.app.admin.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.Language;
import com.app.admin.repository.LanguageRepository;

@Service
public class LanguageService {
    private final LanguageRepository repository;
    public LanguageService(LanguageRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<Language> findAll() { return repository.findAll(Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("name"))); }
    @Transactional(readOnly = true) public Language findById(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Language", id)); }
    @Transactional public Language create(Language value) { value.setId(null); value.setCreatedAt(null); value.setUpdatedAt(null); return repository.saveAndFlush(value); }
    @Transactional public Language update(Long id, Language value) { Language current = findById(id); current.setCode(value.getCode()); current.setName(value.getName()); current.setNativeName(value.getNativeName()); current.setDisplayOrder(value.getDisplayOrder()); current.setActive(value.getActive()); return repository.saveAndFlush(current); }
    @Transactional public void delete(Long id) { repository.delete(findById(id)); repository.flush(); }
}
