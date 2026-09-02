package com.app.admin.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.Event;
import com.app.admin.model.EventStatus;
import com.app.admin.repository.EventRepository;

@Service
public class EventService {
    private final EventRepository repository;

    public EventService(EventRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Event> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "startDate"));
    }

    @Transactional(readOnly = true)
    public Event findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
    }

    @Transactional
    public Event create(Event request) {
        request.setId(null);
        request.setCreatedAt(null);
        request.setStatus(EventStatus.DRAFT);
        request.setStatusChangedAt(LocalDateTime.now());
        clearReviewFields(request);
        normalizeBooleans(request);
        return repository.saveAndFlush(request);
    }

    @Transactional
    public Event update(Long id, Event request) {
        Event current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        BeanUtils.copyProperties(request, current, "id", "createdAt", "updatedAt", "status",
                "submittedAt", "reviewedAt", "reviewedByUserId", "reviewReason",
                "statusChangedAt", "publishedAt", "cancelledAt");
        normalizeBooleans(current);
        return repository.saveAndFlush(current);
    }

    @Transactional
    public void delete(Long id) {
        Event current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        repository.delete(current);
        repository.flush();
    }

    private void normalizeBooleans(Event event) {
        if (event.getIsOnline() == null) {
            event.setIsOnline(false);
        }
        if (event.getIsFeatured() == null) {
            event.setIsFeatured(false);
        }
        if (event.getDeliveryMode() == null || event.getDeliveryMode().isBlank()) {
            event.setDeliveryMode("IN_PERSON");
        }
    }

    private void clearReviewFields(Event event) {
        event.setReviewedAt(null);
        event.setReviewedByUserId(null);
        event.setReviewReason(null);
    }
}
