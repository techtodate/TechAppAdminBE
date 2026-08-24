package com.app.admin.service;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.MedicalUpdate;
import com.app.admin.repository.MedicalUpdateRepository;

@Service
public class MedicalUpdateService {
    private final MedicalUpdateRepository repository;

    public MedicalUpdateService(MedicalUpdateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MedicalUpdate> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public MedicalUpdate findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalUpdate", id));
    }

    @Transactional
    public MedicalUpdate create(MedicalUpdate request) {
        request.setId(null);
        request.setCreatedAt(null);
        return repository.saveAndFlush(request);
    }

    @Transactional
    public MedicalUpdate update(Long id, MedicalUpdate request) {
        MedicalUpdate current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalUpdate", id));
        BeanUtils.copyProperties(request, current, "id", "createdAt");
        return repository.saveAndFlush(current);
    }

    @Transactional
    public void delete(Long id) {
        MedicalUpdate current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalUpdate", id));
        repository.delete(current);
        repository.flush();
    }
}
