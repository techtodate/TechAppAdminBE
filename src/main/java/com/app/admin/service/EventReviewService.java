package com.app.admin.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.dto.EventReviewRequest;
import com.app.admin.dto.BulkEventReviewRequest;
import com.app.admin.dto.StartEventReviewRequest;
import com.app.admin.dto.EventDetailsResponse;
import com.app.admin.dto.UpdateEventStatusRequest;
import com.app.admin.exception.InvalidEventTransitionException;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.Event;
import com.app.admin.model.EventStatus;
import com.app.admin.repository.EventRepository;

@Service
public class EventReviewService {
    private final EventRepository repository;
    private final Clock clock;

    @Autowired
    public EventReviewService(EventRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    EventReviewService(EventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Event> findUnderReview() {
        return repository.findByStatus(
                EventStatus.UNDER_REVIEW,
                Sort.by(Sort.Direction.ASC, "submittedAt").and(Sort.by("id")));
    }

    @Transactional(readOnly = true)
    public List<Event> findReviewQueue() {
        return repository.findByStatusIn(
                List.of(EventStatus.SUBMITTED, EventStatus.UNDER_REVIEW),
                Sort.by(Sort.Direction.ASC, "submittedAt").and(Sort.by("id")));
    }

    @Transactional(readOnly = true)
    public EventDetailsResponse findDetails(Long eventId) {
        Event event = repository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        return new EventDetailsResponse(event, allowedActions(event.getStatus()));
    }

    @Transactional
    public Event startReview(Long eventId, StartEventReviewRequest request) {
        Event event = repository.findWithLockById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        if (event.getStatus() != EventStatus.SUBMITTED) {
            throw new InvalidEventTransitionException(
                    "Event " + eventId + " cannot enter review because its current status is "
                            + event.getStatus());
        }

        event.setStatus(EventStatus.UNDER_REVIEW);
        event.setReviewedByUserId(request.reviewerUserId());
        event.setStatusChangedAt(LocalDateTime.now(clock));
        return repository.saveAndFlush(event);
    }

    @Transactional
    public Event updateStatus(Long eventId, UpdateEventStatusRequest request) {
        Event event = repository.findWithLockById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        EventStatus target = request.status();
        if (!isAllowedTransition(event.getStatus(), target)) {
            throw invalidTransition(event, target);
        }

        LocalDateTime changedAt = LocalDateTime.now(clock);
        if (target == EventStatus.UNDER_REVIEW) {
            requireReason(request.reason());
            event.setReviewReason(request.reason().trim());
            setReviewerIfProvided(event, request.reviewerUserId());
        } else if (target == EventStatus.REJECTED) {
            requireReason(request.reason());
            event.setReviewReason(request.reason().trim());
            setReviewerIfProvided(event, request.reviewerUserId());
            event.setReviewedAt(changedAt);
        } else if (target == EventStatus.APPROVED) {
            event.setReviewReason(null);
            setReviewerIfProvided(event, request.reviewerUserId());
            event.setReviewedAt(changedAt);
        } else if (target == EventStatus.PUBLISHED) {
            event.setPublishedAt(changedAt);
        } else if (target == EventStatus.CANCELLED) {
            event.setCancelledAt(changedAt);
        }

        event.setStatus(target);
        event.setStatusChangedAt(changedAt);
        return repository.saveAndFlush(event);
    }

    @Transactional
    public Event approve(Long eventId, EventReviewRequest request) {
        return review(eventId, request, EventStatus.APPROVED);
    }

    @Transactional
    public Event reject(Long eventId, EventReviewRequest request) {
        return review(eventId, request, EventStatus.REJECTED);
    }

    @Transactional
    public List<Event> approveBulk(BulkEventReviewRequest request) {
        return reviewBulk(request, EventStatus.APPROVED);
    }

    @Transactional
    public List<Event> rejectBulk(BulkEventReviewRequest request) {
        return reviewBulk(request, EventStatus.REJECTED);
    }

    private Event review(Long eventId, EventReviewRequest request, EventStatus decision) {
        Event event = repository.findWithLockById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        LocalDateTime reviewedAt = LocalDateTime.now(clock);
        applyDecision(event, decision, request.reviewerUserId(), request.reason(), reviewedAt);
        return repository.saveAndFlush(event);
    }

    private List<Event> reviewBulk(BulkEventReviewRequest request, EventStatus decision) {
        List<Long> eventIds = new LinkedHashSet<>(request.eventIds()).stream().sorted().toList();
        List<Event> events = repository.findAllWithLockByIdIn(eventIds);

        if (events.size() != eventIds.size()) {
            List<Long> foundIds = events.stream().map(Event::getId).toList();
            List<Long> missingIds = eventIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new InvalidEventTransitionException("Events not found: " + missingIds);
        }

        LocalDateTime reviewedAt = LocalDateTime.now(clock);
        events.forEach(event -> applyDecision(
                event, decision, request.reviewerUserId(), request.reason(), reviewedAt));
        return repository.saveAllAndFlush(events);
    }

    private void applyDecision(
            Event event,
            EventStatus decision,
            Long reviewerUserId,
            String reason,
            LocalDateTime reviewedAt) {
        if (event.getStatus() != EventStatus.UNDER_REVIEW) {
            throw new InvalidEventTransitionException(
                    "Event " + event.getId() + " cannot be " + decision.name().toLowerCase()
                            + " because its current status is " + event.getStatus());
        }
        event.setStatus(decision);
        event.setReviewReason(reason.trim());
        event.setReviewedByUserId(reviewerUserId);
        event.setReviewedAt(reviewedAt);
        event.setStatusChangedAt(reviewedAt);
    }

    private List<String> allowedActions(EventStatus status) {
        return switch (status) {
            case SUBMITTED -> List.of("START_REVIEW");
            case UNDER_REVIEW -> List.of("APPROVE", "REJECT");
            case APPROVED -> List.of("PUBLISH");
            case PUBLISHED -> List.of("OPEN_REGISTRATION", "CANCEL");
            case REGISTRATION_OPEN -> List.of("CLOSE_REGISTRATION", "CANCEL");
            case REGISTRATION_CLOSED -> List.of("GO_LIVE", "CANCEL");
            case LIVE -> List.of("COMPLETE", "CANCEL");
            default -> List.of();
        };
    }

    private InvalidEventTransitionException invalidTransition(Event event, EventStatus target) {
        return new InvalidEventTransitionException(
                "Event " + event.getId() + " cannot transition from " + event.getStatus()
                        + " to " + target);
    }

    private boolean isAllowedTransition(EventStatus current, EventStatus target) {
        return switch (current) {
            case SUBMITTED -> target == EventStatus.UNDER_REVIEW;
            case UNDER_REVIEW -> target == EventStatus.APPROVED || target == EventStatus.REJECTED;
            case APPROVED -> target == EventStatus.PUBLISHED;
            case PUBLISHED -> target == EventStatus.REGISTRATION_OPEN || target == EventStatus.CANCELLED;
            case REGISTRATION_OPEN -> target == EventStatus.REGISTRATION_CLOSED || target == EventStatus.CANCELLED;
            case REGISTRATION_CLOSED -> target == EventStatus.LIVE || target == EventStatus.CANCELLED;
            case LIVE -> target == EventStatus.COMPLETED || target == EventStatus.CANCELLED;
            default -> false;
        };
    }

    private void requireReason(String reason) {
        if (reason == null || reason.trim().length() < 10) {
            throw new IllegalArgumentException("reason must contain at least 10 characters");
        }
    }

    private void setReviewerIfProvided(Event event, Long reviewerUserId) {
        if (reviewerUserId != null) {
            event.setReviewedByUserId(reviewerUserId);
        }
    }
}
