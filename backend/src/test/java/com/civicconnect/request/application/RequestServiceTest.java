package com.civicconnect.request.application;

import com.civicconnect.audit.application.AuditService;
import com.civicconnect.request.domain.RequestCategory;
import com.civicconnect.request.domain.RequestStatusTransitionPolicy;
import com.civicconnect.request.domain.ServiceRequest;
import com.civicconnect.request.persistence.ServiceRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RequestServiceTest {
    @Test
    void createWritesRequestAndMandatoryAuditInSameServiceOperation() {
        ServiceRequestRepository repository = mock(ServiceRequestRepository.class);
        AuditService audit = mock(AuditService.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-29T00:00:00Z"), ZoneOffset.UTC);
        RequestService service = new RequestService(repository, audit,
                new RequestStatusTransitionPolicy(), events, clock);

        RequestView result = service.create("requester-1",
                new CreateRequestCommand("Broken light", "Light outside room is not working", RequestCategory.FACILITY_FAULT));

        assertEquals("requester-1", result.requesterId());
        assertEquals("SUBMITTED", result.status().name());
        verify(repository).save(any(ServiceRequest.class));
        verify(audit).record(eq(result.id()), eq("requester-1"), eq("REQUEST_CREATED"),
                isNull(), eq("SUBMITTED"), anyString(), eq(clock.instant()));
    }
}
