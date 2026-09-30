package com.civicconnect.request.application;

import com.civicconnect.audit.application.AuditService;
import com.civicconnect.request.application.event.RequestStatusChangedEvent;
import com.civicconnect.request.domain.RequestStatus;
import com.civicconnect.request.domain.RequestStatusTransitionPolicy;
import com.civicconnect.request.domain.ServiceRequest;
import com.civicconnect.request.persistence.ServiceRequestRepository;
import com.civicconnect.shared.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RequestService {
    private final ServiceRequestRepository requests;
    private final AuditService audit;
    private final RequestStatusTransitionPolicy transitionPolicy;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RequestService(ServiceRequestRepository requests,
                          AuditService audit,
                          RequestStatusTransitionPolicy transitionPolicy,
                          ApplicationEventPublisher events) {
        this(requests, audit, transitionPolicy, events, Clock.systemUTC());
    }

    RequestService(ServiceRequestRepository requests,
                   AuditService audit,
                   RequestStatusTransitionPolicy transitionPolicy,
                   ApplicationEventPublisher events,
                   Clock clock) {
        this.requests = requests;
        this.audit = audit;
        this.transitionPolicy = transitionPolicy;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public RequestView create(String requesterId, CreateRequestCommand command) {
        Instant now = clock.instant();
        ServiceRequest request = ServiceRequest.create(requesterId, command.title(), command.description(), command.category(), now);
        requests.save(request);
        audit.record(request.getId(), requesterId, "REQUEST_CREATED", null,
                RequestStatus.SUBMITTED.name(), "Request submitted", now);
        return RequestView.from(request);
    }

    @Transactional(readOnly = true)
    public List<RequestView> myRequests(String requesterId) {
        return requests.findByRequesterIdOrderByCreatedAtDesc(requesterId)
                .stream().map(RequestView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RequestView> allRequests() {
        return requests.findAll().stream().map(RequestView::from).toList();
    }

    @Transactional
    public RequestView acceptOwnership(UUID requestId, String staffId) {
        ServiceRequest request = get(requestId);
        RequestStatus before = request.getStatus();
        Instant now = clock.instant();
        request.assignTo(staffId, now);
        requests.save(request);
        audit.record(requestId, staffId, "OWNERSHIP_ACCEPTED", before.name(), request.getStatus().name(), null, now);
        if (before != request.getStatus()) {
            events.publishEvent(new RequestStatusChangedEvent(requestId, request.getRequesterId(), before, request.getStatus(), now));
        }
        return RequestView.from(request);
    }

    @Transactional
    public RequestView changeStatus(UUID requestId, String actor, ChangeStatusCommand command) {
        try {
            ServiceRequest request = get(requestId);
            RequestStatus before = request.getStatus();
            transitionPolicy.validate(before, command.targetStatus());

            Instant now = clock.instant();
            request.changeStatus(command.targetStatus(), now);
            requests.saveAndFlush(request);

            // Correctness-critical audit evidence is inside the same transaction.
            audit.record(requestId, actor, "STATUS_CHANGED", before.name(), command.targetStatus().name(), command.comment(), now);

            // Secondary reactions observe the event after commit.
            events.publishEvent(new RequestStatusChangedEvent(requestId, request.getRequesterId(), before, command.targetStatus(), now));
            return RequestView.from(request);
        } catch (OptimisticLockingFailureException ex) {
            throw new com.civicconnect.shared.ConflictException(
                    "The request changed while you were editing it. Reload and try again.");
        }
    }

    private ServiceRequest get(UUID requestId) {
        return requests.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request " + requestId + " was not found."));
    }
}
