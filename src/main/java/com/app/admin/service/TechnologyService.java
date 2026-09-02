package com.app.admin.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.Technology;
import com.app.admin.repository.TechnologyRepository;

@Service
public class TechnologyService {
    private final TechnologyRepository repository;
    private final MediaStorage storage;
    private final TopicImageLifecycle imageLifecycle;

    public TechnologyService(TechnologyRepository repository, MediaStorage storage,
            TopicImageLifecycle imageLifecycle) {
        this.repository = repository;
        this.storage = storage;
        this.imageLifecycle = imageLifecycle;
    }

    @Transactional(readOnly = true)
    public List<Technology> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(this::withImageUrl).toList();
    }

    @Transactional(readOnly = true)
    public Technology findById(Long id) {
        return withImageUrl(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technology", id)));
    }

    @Transactional
    public Technology create(Technology request) {
        request.setId(null);
        request.setCreatedAt(null);
        return withImageUrl(repository.saveAndFlush(request));
    }

    @Transactional
    public Technology update(Long id, Technology request) {
        Technology current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technology", id));
        current.setName(request.getName());
        current.setDescription(request.getDescription());
        current.setCategoryId(request.getCategoryId());
        current.setFieldId(request.getFieldId());
        return withImageUrl(repository.saveAndFlush(current));
    }

    @Transactional
    public void delete(Long id) {
        Technology current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technology", id));
        repository.delete(current);
        repository.flush();
        imageLifecycle.deleteAfterCommit(current.getImageKey());
    }

    private Technology withImageUrl(Technology topic) {
        topic.setImageUrl(storage.publicUrl(topic.getImageKey()));
        return topic;
    }
}
