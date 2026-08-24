package com.app.admin.service;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.Category;
import com.app.admin.repository.CategoryRepository;

@Service
public class CategoryService {
    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    @Transactional
    public Category create(Category request) {
        request.setId(null);
        request.setCreatedAt(null);
        return repository.saveAndFlush(request);
    }

    @Transactional
    public Category update(Long id, Category request) {
        Category current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        BeanUtils.copyProperties(request, current, "id", "createdAt");
        return repository.saveAndFlush(current);
    }

    @Transactional
    public void delete(Long id) {
        Category current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        repository.delete(current);
        repository.flush();
    }
}
