package com.app.admin.service;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.MedicalSubject;
import com.app.admin.repository.MedicalSubjectRepository;

@Service
public class MedicalSubjectService {
    private final MedicalSubjectRepository repository;
    private final MediaStorage storage;
    private final TopicImageLifecycle imageLifecycle;

    public MedicalSubjectService(MedicalSubjectRepository repository, MediaStorage storage,
            TopicImageLifecycle imageLifecycle) {
        this.repository = repository;
        this.storage = storage;
        this.imageLifecycle = imageLifecycle;
    }

    @Transactional(readOnly = true)
    public List<MedicalSubject> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(this::withImageUrl).toList();
    }

    @Transactional(readOnly = true)
    public MedicalSubject findById(Long id) {
        return withImageUrl(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalSubject", id)));
    }

    @Transactional
    public MedicalSubject create(MedicalSubject request) {
        request.setId(null);
        request.setCreatedAt(null);
        return withImageUrl(repository.saveAndFlush(request));
    }

    @Transactional
    public MedicalSubject update(Long id, MedicalSubject request) {
        MedicalSubject current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalSubject", id));
        BeanUtils.copyProperties(request, current, "id", "createdAt", "updatedAt", "imageKey", "imageUrl");
        return withImageUrl(repository.saveAndFlush(current));
    }

    @Transactional
    public void delete(Long id) {
        MedicalSubject current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalSubject", id));
        repository.delete(current);
        repository.flush();
        imageLifecycle.deleteAfterCommit(current.getImageKey());
    }

    private MedicalSubject withImageUrl(MedicalSubject topic) {
        topic.setImageUrl(storage.publicUrl(topic.getImageKey()));
        return topic;
    }
}
