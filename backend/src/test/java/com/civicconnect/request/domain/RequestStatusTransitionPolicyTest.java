package com.civicconnect.request.domain;

import com.civicconnect.shared.ConflictException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestStatusTransitionPolicyTest {
    private final RequestStatusTransitionPolicy policy = new RequestStatusTransitionPolicy();

    @Test
    void allowsProvisionalSubmittedToAcceptedTransition() {
        assertDoesNotThrow(() -> policy.validate(RequestStatus.SUBMITTED, RequestStatus.ACCEPTED));
    }

    @Test
    void rejectsClosedToInProgressTransition() {
        assertThrows(ConflictException.class,
                () -> policy.validate(RequestStatus.CLOSED, RequestStatus.IN_PROGRESS));
    }
}
