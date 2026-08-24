package com.app.admin.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.ProfileType;
import com.app.admin.repository.ProfileTypeRepository;

@Service
public class ProfileTypeService {
    private final ProfileTypeRepository repository;

    public ProfileTypeService(ProfileTypeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ProfileType> findAll() {
        return repository.findAll(Sort.by(
                Sort.Order.asc("displayOrder"),
                Sort.Order.asc("name")));
    }

    @Transactional(readOnly = true)
    public ProfileType findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProfileType", id));
    }

    @Transactional
    public ProfileType create(ProfileType request) {
        request.setId(null);
        request.setCreatedAt(null);
        request.setUpdatedAt(null);
        return repository.saveAndFlush(request);
    }

    @Transactional
    public ProfileType update(Long id, ProfileType request) {
        ProfileType current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProfileType", id));
        current.setCode(request.getCode());
        current.setName(request.getName());
        current.setDescription(request.getDescription());
        current.setDisplayOrder(request.getDisplayOrder());
        current.setActive(request.getActive());
        return repository.saveAndFlush(current);
    }

    @Transactional
    public void delete(Long id) {
        ProfileType current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProfileType", id));
        repository.delete(current);
        repository.flush();
    }
}
