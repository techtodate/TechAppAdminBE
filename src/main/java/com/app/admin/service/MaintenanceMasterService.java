package com.app.admin.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.MaintenanceMaster;
import com.app.admin.repository.MaintenanceMasterRepository;

public abstract class MaintenanceMasterService<T extends MaintenanceMaster> {
    private final MaintenanceMasterRepository<T> repository;
    private final String entityName;

    protected MaintenanceMasterService(MaintenanceMasterRepository<T> repository, String entityName) {
        this.repository = repository;
        this.entityName = entityName;
    }

    @Transactional(readOnly = true)
    public List<T> findAll() {
        return repository.findAll(Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("name")));
    }

    @Transactional(readOnly = true)
    public T findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(entityName, id));
    }

    @Transactional
    public T create(T request) {
        request.setId(null);
        request.setCreatedAt(null);
        request.setUpdatedAt(null);
        return repository.saveAndFlush(request);
    }

    @Transactional
    public T update(Long id, T request) {
        T current = findById(id);
        current.setCode(request.getCode());
        current.setName(request.getName());
        current.setDescription(request.getDescription());
        current.setDisplayOrder(request.getDisplayOrder());
        current.setActive(request.getActive());
        return repository.saveAndFlush(current);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
        repository.flush();
    }
}
