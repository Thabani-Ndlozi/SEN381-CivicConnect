package com.civicconnect.request.application;

import com.civicconnect.audit.application.AuditService;
import com.civicconnect.request.domain.RequestCategory;
import com.civicconnect.request.domain.RequestStatusTransitionPolicy;
import com.civicconnect.request.domain.ServiceRequest;
import com.civicconnect.request.persistence.ServiceRequestRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Location regression tests: coordinates and address must survive create + response mapping. */
class RequestCoordinatesTest {
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private CreateRequestCommand command(Double lat, Double lng, String location) {
        return new CreateRequestCommand("Streetlight", "Light out on the corner", RequestCategory.MAINTENANCE,
                location, "NORMAL", lat, lng);
    }
    @Test void validCoordinatesAndManualAddressBothAccepted() {
        assertTrue(validator.validate(command(-25.7479, 28.2293, "Pretoria, South Africa")).isEmpty());
        assertTrue(validator.validate(command(null, null, "Pretoria, South Africa")).isEmpty());
    }
    @Test void rejectsPartialCoordinates() {
        assertFalse(validator.validate(command(-25.7479, null, "Pretoria")).isEmpty());
        assertFalse(validator.validate(command(null, 28.2293, "Pretoria")).isEmpty());
    }
    @Test void rejectsOutOfRangeCoordinatesOrMissingAddress() {
        assertFalse(validator.validate(command(-90.01, 28.2293, "Pretoria")).isEmpty());
        assertFalse(validator.validate(command(10.0, 180.01, "Pretoria")).isEmpty());
        assertFalse(validator.validate(command(0.0, 0.0, "  ")).isEmpty());
    }
    @Test void persistedRequestResponseContainsConfirmedPin() {
        ServiceRequestRepository repository = mock(ServiceRequestRepository.class);
        AuditService audit = mock(AuditService.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        RequestService service = new RequestService(repository, audit, new RequestStatusTransitionPolicy(), events);
        AtomicReference<ServiceRequest> saved = new AtomicReference<>();
        when(repository.save(any(ServiceRequest.class))).thenAnswer(call -> {
            saved.set(call.getArgument(0)); return call.getArgument(0);
        });
        RequestView view = service.create("requester@example.com", command(-25.7479, 28.2293, "Pretoria"));
        assertEquals(Double.valueOf(-25.7479), view.latitude());
        assertEquals(Double.valueOf(28.2293), view.longitude());
        assertEquals("Pretoria", view.location());
        assertEquals(view.latitude(), saved.get().getLatitude());
        assertEquals(view.longitude(), saved.get().getLongitude());
    }
    @Test void legacyRequestWithoutPinRemainsValid() {
        var legacy = com.civicconnect.request.domain.ServiceRequest.create(
                "requester@example.com", "Old issue", "Legacy description", RequestCategory.OTHER, java.time.Instant.now());
        assertNull(RequestView.from(legacy).latitude());
        assertNull(RequestView.from(legacy).longitude());
    }
}
