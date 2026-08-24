package com.app.admin.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.UserType;
import com.app.admin.repository.UserTypeRepository;

@Service
public class UserTypeService {
    private final UserTypeRepository repository;

    public UserTypeService(UserTypeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<UserType> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public UserType findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserType", id));
    }

    @Transactional
    public UserType create(UserType request) {
        request.setId(null);
        request.setCreatedAt(null);
        return repository.saveAndFlush(request);
    }

    @Transactional
    public UserType update(Long id, UserType request) {
        UserType current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserType", id));
        current.setFieldId(request.getFieldId());
        current.setCode(request.getCode());
        current.setName(request.getName());
        current.setDisplayOrder(request.getDisplayOrder());
        current.setActive(request.getActive());
        current.setProfileType(request.getProfileType());
        return repository.saveAndFlush(current);
    }

    @Transactional
    public void delete(Long id) {
        UserType current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserType", id));
        repository.delete(current);
        repository.flush();
    }
}
