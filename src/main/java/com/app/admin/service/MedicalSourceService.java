package com.app.admin.service;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.MedicalSource;
import com.app.admin.repository.MedicalSourceRepository;

@Service
public class MedicalSourceService {
    private final MedicalSourceRepository repository;

    public MedicalSourceService(MedicalSourceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MedicalSource> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public MedicalSource findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalSource", id));
    }

    @Transactional
    public MedicalSource create(MedicalSource request) {
        request.setId(null);
        request.setCreatedAt(null);
        return repository.saveAndFlush(request);
    }

    @Transactional
    public MedicalSource update(Long id, MedicalSource request) {
        MedicalSource current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalSource", id));
        BeanUtils.copyProperties(request, current, "id", "createdAt");
        return repository.saveAndFlush(current);
    }

    @Transactional
    public void delete(Long id) {
        MedicalSource current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalSource", id));
        repository.delete(current);
        repository.flush();
    }
}
