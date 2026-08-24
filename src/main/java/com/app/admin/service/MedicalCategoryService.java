package com.app.admin.service;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.MedicalCategory;
import com.app.admin.repository.MedicalCategoryRepository;

@Service
public class MedicalCategoryService {
    private final MedicalCategoryRepository repository;

    public MedicalCategoryService(MedicalCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MedicalCategory> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public MedicalCategory findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalCategory", id));
    }

    @Transactional
    public MedicalCategory create(MedicalCategory request) {
        request.setId(null);
        request.setCreatedAt(null);
        return repository.saveAndFlush(request);
    }

    @Transactional
    public MedicalCategory update(Long id, MedicalCategory request) {
        MedicalCategory current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalCategory", id));
        BeanUtils.copyProperties(request, current, "id", "createdAt", "updatedAt");
        return repository.saveAndFlush(current);
    }

    @Transactional
    public void delete(Long id) {
        MedicalCategory current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalCategory", id));
        repository.delete(current);
        repository.flush();
    }
}
