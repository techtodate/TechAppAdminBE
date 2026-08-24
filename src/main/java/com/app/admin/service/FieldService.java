package com.app.admin.service;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.Field;
import com.app.admin.repository.FieldRepository;

@Service
public class FieldService {
    private final FieldRepository repository;

    public FieldService(FieldRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Field> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public Field findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Field", id));
    }

    @Transactional
    public Field create(Field request) {
        request.setId(null);
        request.setCreatedAt(null);
        return repository.saveAndFlush(request);
    }

    @Transactional
    public Field update(Long id, Field request) {
        Field current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Field", id));
        BeanUtils.copyProperties(request, current, "id", "createdAt", "updatedAt");
        return repository.saveAndFlush(current);
    }

    @Transactional
    public void delete(Long id) {
        Field current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Field", id));
        repository.delete(current);
        repository.flush();
    }
}
