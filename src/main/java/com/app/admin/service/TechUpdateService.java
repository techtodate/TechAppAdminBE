package com.app.admin.service;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.TechUpdate;
import com.app.admin.repository.TechUpdateRepository;

@Service
public class TechUpdateService {
    private final TechUpdateRepository repository;

    public TechUpdateService(TechUpdateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TechUpdate> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public TechUpdate findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TechUpdate", id));
    }

    @Transactional
    public TechUpdate create(TechUpdate request) {
        request.setId(null);
        request.setCreatedAt(null);
        return repository.saveAndFlush(request);
    }

    @Transactional
    public TechUpdate update(Long id, TechUpdate request) {
        TechUpdate current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TechUpdate", id));
        BeanUtils.copyProperties(request, current, "id", "createdAt");
        return repository.saveAndFlush(current);
    }

    @Transactional
    public void delete(Long id) {
        TechUpdate current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TechUpdate", id));
        repository.delete(current);
        repository.flush();
    }
}
