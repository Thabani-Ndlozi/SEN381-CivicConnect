package com.civicconnect.request.domain;

import com.civicconnect.shared.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class ServiceRequestRulesTest {
    private ServiceRequest fresh() {
        return ServiceRequest.create("requester-1", "Broken streetlight", "Streetlight broken by gate", RequestCategory.FACILITY_FAULT, Instant.parse("2026-10-12T00:00:00Z"));
    }
    @Test void initialRecordPreservesCategoryAndSubmittedState() {
        ServiceRequest r = fresh();
        assertEquals(RequestCategory.FACILITY_FAULT, r.getCategory());
        assertEquals(RequestStatus.SUBMITTED, r.getStatus());
        assertNull(r.getOwnerId());
    }
    @Test void acceptanceRecordsOwnerAndStatus() {
        ServiceRequest r = fresh(); r.assignTo("staff-1", Instant.parse("2026-10-12T01:00:00Z"));
        assertEquals("staff-1", r.getOwnerId());
        assertEquals(RequestStatus.ACCEPTED, r.getStatus());
    }
    @Test void conflictingStaffCannotTakeOverAcceptedRequest() {
        ServiceRequest r = fresh(); r.assignTo("staff-1", Instant.now());
        assertThrows(ConflictException.class, () -> r.assignTo("staff-2", Instant.now()));
        assertEquals("staff-1", r.getOwnerId());
    }
    @Test void unassignedRequestRejectsStatusChangeByStaff() {
        ServiceRequest r = fresh();
        assertThrows(AccessDeniedException.class, () -> r.assertOwnedBy("staff-1"));
    }
    @Test void onlyCurrentOwnerMayChangeStatus() {
        ServiceRequest r = fresh(); r.assignTo("staff-1", Instant.now());
        assertDoesNotThrow(() -> r.assertOwnedBy("staff-1"));
        assertThrows(AccessDeniedException.class, () -> r.assertOwnedBy("staff-2"));
    }
    @Test void completedRequestCannotBeReaccepted() {
        ServiceRequest r = fresh(); r.assignTo("staff-1", Instant.now());
        r.changeStatus(RequestStatus.CLOSED, Instant.now());
        assertThrows(ConflictException.class, () -> r.assignTo("staff-2", Instant.now()));
    }
}
