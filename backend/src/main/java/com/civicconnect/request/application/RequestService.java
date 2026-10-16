package com.civicconnect.request.application;

import org.springframework.beans.factory.annotation.Autowired;
import com.civicconnect.audit.application.AuditService;
import com.civicconnect.audit.application.AuditView;
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

    @Autowired
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
        request.setDetails(command.location(), command.priority(), command.latitude(), command.longitude());
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
        requests.saveAndFlush(request);
        audit.record(requestId, staffId, "OWNERSHIP_ACCEPTED", before.name(), request.getStatus().name(), null, now);
        if (before != request.getStatus()) {
            events.publishEvent(new RequestStatusChangedEvent(requestId, request.getRequesterId(), before, request.getStatus(), now, request.getTitle()));
        }
        return RequestView.from(request);
    }

    @Transactional
    public RequestView changeStatus(UUID requestId, String actor, ChangeStatusCommand command) {
        try {
            ServiceRequest request = get(requestId);
            if (!(request.getStatus() == RequestStatus.SUBMITTED &&
                  command.targetStatus() == RequestStatus.REJECTED)) {
                boolean manager = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null &&
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .anyMatch(a -> (a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_SUPERUSER")));
                if (!manager) request.assertOwnedBy(actor);
            }
            if (!Long.valueOf(request.getVersion()).equals(command.version())) {
                throw new com.civicconnect.shared.ConflictException("The request version is stale. Reload and try again.");
            }
            RequestStatus before = request.getStatus();
            transitionPolicy.validate(before, command.targetStatus());
            if ((command.targetStatus() == RequestStatus.REJECTED ||
                 command.targetStatus() == RequestStatus.RESOLVED ||
                 command.targetStatus() == RequestStatus.CLOSED) &&
                (command.comment() == null || command.comment().isBlank())) {
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "A reason or resolution comment is required.");
            }

            Instant now = clock.instant();
            request.changeStatus(command.targetStatus(), now);
            requests.saveAndFlush(request);

            // Correctness-critical audit evidence is inside the same transaction.
            audit.record(requestId, actor, "STATUS_CHANGED", before.name(), command.targetStatus().name(), command.comment(), now);

            // Secondary reactions observe the event after commit.
            events.publishEvent(new RequestStatusChangedEvent(requestId, request.getRequesterId(), before, command.targetStatus(), now, request.getTitle()));
            return RequestView.from(request);
        } catch (OptimisticLockingFailureException ex) {
            throw new com.civicconnect.shared.ConflictException(
                    "The request changed while you were editing it. Reload and try again.");
        }
    }

    @Transactional(readOnly = true)
    public List<AuditView> requesterHistory(UUID requestId, String requesterId) {
        ServiceRequest request = get(requestId);
        // A 404 prevents disclosure that another requester has an ID.
        if (!request.getRequesterId().equals(requesterId)) {
            throw new NotFoundException("Request " + requestId + " was not found.");
        }
        return audit.historyForRequester(requestId);
    }

    @Transactional(readOnly = true)
    public List<AuditView> managementHistory(UUID requestId) {
        get(requestId);
        return audit.historyForManagement(requestId);
    }

    @Transactional(readOnly = true)
    public RequestView staffDetail(UUID requestId) {
        return RequestView.from(get(requestId));
    }

    private ServiceRequest get(UUID requestId) {
        return requests.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request " + requestId + " was not found."));
    }
}
