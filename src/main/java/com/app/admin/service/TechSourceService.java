package com.app.admin.service;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.TechSource;
import com.app.admin.repository.TechSourceRepository;

@Service
public class TechSourceService {
    private final TechSourceRepository repository;

    public TechSourceService(TechSourceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TechSource> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public TechSource findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TechSource", id));
    }

    @Transactional
    public TechSource create(TechSource request) {
        request.setId(null);
        request.setCreatedAt(null);
        return repository.saveAndFlush(request);
    }

    @Transactional
    public TechSource update(Long id, TechSource request) {
        TechSource current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TechSource", id));
        BeanUtils.copyProperties(request, current, "id", "createdAt");
        return repository.saveAndFlush(current);
    }

    @Transactional
    public void delete(Long id) {
        TechSource current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TechSource", id));
        repository.delete(current);
        repository.flush();
    }
}
