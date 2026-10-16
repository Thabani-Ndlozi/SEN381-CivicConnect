package com.civicconnect.request.application;

import com.civicconnect.audit.application.AuditService;
import com.civicconnect.request.application.event.RequestStatusChangedEvent;
import com.civicconnect.request.domain.*;
import com.civicconnect.request.persistence.ServiceRequestRepository;
import com.civicconnect.shared.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RequestServiceM3Test {
    private final ServiceRequestRepository repo = mock(ServiceRequestRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T00:00:00Z"), ZoneOffset.UTC);
    private final RequestService service = new RequestService(repo, audit, new RequestStatusTransitionPolicy(), events, clock);
    private ServiceRequest fresh() { return ServiceRequest.create("requester", "Broken lamp", "Gate", RequestCategory.FACILITY_FAULT, clock.instant()); }
    @Test void missingRequestDoesNotCreateAudit() {
        assertThrows(NotFoundException.class, () -> service.acceptOwnership(UUID.randomUUID(), "staff"));
        verifyNoInteractions(audit, events);
    }
    @Test void acceptingRequestWritesAuditAndPublishesChange() {
        ServiceRequest r = fresh(); when(repo.findById(r.getId())).thenReturn(Optional.of(r));
        service.acceptOwnership(r.getId(), "staff");
        verify(audit).record(eq(r.getId()), eq("staff"), eq("OWNERSHIP_ACCEPTED"), eq("SUBMITTED"), eq("ACCEPTED"), isNull(), eq(clock.instant()));
        verify(events).publishEvent(any(RequestStatusChangedEvent.class));
    }
    @Test void invalidTransitionLeavesAuditUntouched() {
        ServiceRequest r = fresh(); r.assignTo("staff", clock.instant()); when(repo.findById(r.getId())).thenReturn(Optional.of(r));
        assertThrows(com.civicconnect.shared.ConflictException.class,
             () -> service.changeStatus(r.getId(), "staff", new ChangeStatusCommand(RequestStatus.CLOSED, "invalid", r.getVersion())));
        verifyNoInteractions(audit, events);
    }
    @Test void nonOwnerCannotMutateAndWritesNoAudit() {
        ServiceRequest r = fresh(); r.assignTo("staff", clock.instant()); when(repo.findById(r.getId())).thenReturn(Optional.of(r));
        assertThrows(AccessDeniedException.class,
             () -> service.changeStatus(r.getId(), "other", new ChangeStatusCommand(RequestStatus.IN_PROGRESS, "in progress", r.getVersion())));
        verifyNoInteractions(audit, events);
    }
    @Test void foreignRequesterHistoryIsConcealed() {
        ServiceRequest r = fresh(); when(repo.findById(r.getId())).thenReturn(Optional.of(r));
        assertThrows(NotFoundException.class, () -> service.requesterHistory(r.getId(), "stranger"));
        verifyNoInteractions(audit);
    }
    @Test void staleVersionRejectedBeforeAudit() {
        ServiceRequest r = fresh(); r.assignTo("staff", clock.instant()); when(repo.findById(r.getId())).thenReturn(Optional.of(r));
        assertThrows(com.civicconnect.shared.ConflictException.class,
              () -> service.changeStatus(r.getId(), "staff", new ChangeStatusCommand(RequestStatus.IN_PROGRESS, "Begin", 99L)));
        verifyNoInteractions(audit, events);
    }
    @Test void resolutionWithoutExplanationIsRejected() {
        ServiceRequest r = fresh(); r.assignTo("staff", clock.instant()); r.changeStatus(RequestStatus.IN_PROGRESS, clock.instant());
        when(repo.findById(r.getId())).thenReturn(Optional.of(r));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
             () -> service.changeStatus(r.getId(), "staff", new ChangeStatusCommand(RequestStatus.RESOLVED, " ", r.getVersion())));
    }
    @Test void requesterHistoryReadsOwnEntriesOnly() {
        ServiceRequest r = fresh(); when(repo.findById(r.getId())).thenReturn(Optional.of(r));
        service.requesterHistory(r.getId(), "requester"); verify(audit).historyForRequester(r.getId());
    }
}
