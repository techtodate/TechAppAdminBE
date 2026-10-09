package com.app.admin.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.app.admin.dto.BulkEventReviewRequest;
import com.app.admin.dto.EventReviewRequest;
import com.app.admin.dto.UpdateEventStatusRequest;
import com.app.admin.exception.InvalidEventTransitionException;
import com.app.admin.model.Event;
import com.app.admin.model.EventStatus;
import com.app.admin.repository.EventRepository;

class EventReviewServiceTest {
    private final EventRepository repository = mock(EventRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-18T12:00:00Z"), ZoneOffset.UTC);
    private final EventReviewService service = new EventReviewService(repository, clock);
    private static final String REASON = "Admin reviewed the submission";

    static Stream<Arguments> decisions() {
        return Stream.of(EventStatus.SUBMITTED, EventStatus.UNDER_REVIEW)
                .flatMap(source -> Stream.of(EventStatus.APPROVED, EventStatus.REJECTED)
                        .map(target -> Arguments.of(source, target)));
    }

    private Event event(long id, EventStatus status) {
        Event event = new Event();
        event.setId(id);
        event.setStatus(status);
        return event;
    }

    @ParameterizedTest
    @MethodSource("decisions")
    void patchAllowsDirectAndUnderReviewDecisions(EventStatus source, EventStatus target) {
        Event event = event(13L, source);
        when(repository.findWithLockById(13L)).thenReturn(Optional.of(event));
        when(repository.saveAndFlush(event)).thenReturn(event);

        service.updateStatus(13L, new UpdateEventStatusRequest(target, 38L, REASON));

        assertEquals(target, event.getStatus());
        assertEquals(38L, event.getReviewedByUserId());
        assertEquals(LocalDateTime.now(clock), event.getReviewedAt());
        assertEquals(event.getReviewedAt(), event.getStatusChangedAt());
        assertEquals(target == EventStatus.REJECTED ? REASON : null, event.getReviewReason());
    }

    @ParameterizedTest
    @MethodSource("decisions")
    void dedicatedEndpointsAllowBothPendingStatuses(EventStatus source, EventStatus target) {
        Event event = event(13L, source);
        when(repository.findWithLockById(13L)).thenReturn(Optional.of(event));
        EventReviewRequest request = new EventReviewRequest(38L, REASON);
        if (target == EventStatus.APPROVED) service.approve(13L, request);
        else service.reject(13L, request);
        assertEquals(target, event.getStatus());
        verify(repository).saveAndFlush(event);
    }

    @Test
    void bulkDecisionsAcceptMixedPendingStatuses() {
        for (EventStatus target : List.of(EventStatus.APPROVED, EventStatus.REJECTED)) {
            List<Event> events = List.of(event(13L, EventStatus.SUBMITTED), event(14L, EventStatus.UNDER_REVIEW));
            when(repository.findAllWithLockByIdIn(List.of(13L, 14L))).thenReturn(events);
            BulkEventReviewRequest request = new BulkEventReviewRequest(List.of(13L, 14L), 38L, REASON);
            if (target == EventStatus.APPROVED) service.approveBulk(request);
            else service.rejectBulk(request);
            events.forEach(event -> assertEquals(target, event.getStatus()));
        }
    }

    @Test
    void submittedDetailsAdvertiseAllThreeChoices() {
        when(repository.findById(13L)).thenReturn(Optional.of(event(13L, EventStatus.SUBMITTED)));
        assertEquals(List.of("START_REVIEW", "APPROVE", "REJECT"), service.findDetails(13L).allowedActions());
    }

    @Test
    void reviewRemainsOptionalAndRejectionStillRequiresReason() {
        Event event = event(13L, EventStatus.SUBMITTED);
        when(repository.findWithLockById(13L)).thenReturn(Optional.of(event));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateStatus(13L, new UpdateEventStatusRequest(EventStatus.REJECTED, 38L, null)));
        assertEquals(EventStatus.SUBMITTED, event.getStatus());
        service.updateStatus(13L, new UpdateEventStatusRequest(EventStatus.UNDER_REVIEW, 38L, REASON));
        assertEquals(EventStatus.UNDER_REVIEW, event.getStatus());
    }

    @Test
    void decisionsStillRejectOtherLifecycleStatuses() {
        for (EventStatus source : EventStatus.values()) {
            if (source == EventStatus.SUBMITTED || source == EventStatus.UNDER_REVIEW) continue;
            for (EventStatus target : List.of(EventStatus.APPROVED, EventStatus.REJECTED)) {
                Event event = event(13L, source);
                when(repository.findWithLockById(13L)).thenReturn(Optional.of(event));
                assertThrows(InvalidEventTransitionException.class,
                        () -> service.updateStatus(13L, new UpdateEventStatusRequest(target, 38L, REASON)));
                EventReviewRequest request = new EventReviewRequest(38L, REASON);
                assertThrows(InvalidEventTransitionException.class, () -> {
                    if (target == EventStatus.APPROVED) service.approve(13L, request);
                    else service.reject(13L, request);
                });
                assertEquals(source, event.getStatus());
            }
        }
        verify(repository, never()).saveAndFlush(any());
    }
}
