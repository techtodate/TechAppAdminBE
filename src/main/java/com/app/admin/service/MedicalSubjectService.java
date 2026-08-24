package com.app.admin.service;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.MedicalSubject;
import com.app.admin.repository.MedicalSubjectRepository;

@Service
public class MedicalSubjectService {
    private final MedicalSubjectRepository repository;

    public MedicalSubjectService(MedicalSubjectRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MedicalSubject> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public MedicalSubject findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalSubject", id));
    }

    @Transactional
    public MedicalSubject create(MedicalSubject request) {
        request.setId(null);
        request.setCreatedAt(null);
        return repository.saveAndFlush(request);
    }

    @Transactional
    public MedicalSubject update(Long id, MedicalSubject request) {
        MedicalSubject current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalSubject", id));
        BeanUtils.copyProperties(request, current, "id", "createdAt", "updatedAt");
        return repository.saveAndFlush(current);
    }

    @Transactional
    public void delete(Long id) {
        MedicalSubject current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalSubject", id));
        repository.delete(current);
        repository.flush();
    }
}
