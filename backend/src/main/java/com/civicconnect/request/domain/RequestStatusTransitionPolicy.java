package com.civicconnect.request.domain;

import com.civicconnect.shared.ConflictException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * PROVISIONAL M2 rule set used to make the initial functional path testable.
 * M1 assumption A-005 says the exact status-transition catalogue still needs
 * validation. Update this policy through controlled change when that evidence arrives.
 */
@Component
public class RequestStatusTransitionPolicy {
    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED = Map.of(
            RequestStatus.SUBMITTED, Set.of(RequestStatus.ACCEPTED, RequestStatus.REJECTED),
            RequestStatus.ACCEPTED, Set.of(RequestStatus.IN_PROGRESS, RequestStatus.REJECTED),
            RequestStatus.IN_PROGRESS, Set.of(RequestStatus.RESOLVED),
            RequestStatus.RESOLVED, Set.of(RequestStatus.CLOSED, RequestStatus.IN_PROGRESS),
            RequestStatus.REJECTED, Set.of(),
            RequestStatus.CLOSED, Set.of()
    );

    public void validate(RequestStatus current, RequestStatus target) {
        if (!ALLOWED.getOrDefault(current, Set.of()).contains(target)) {
            throw new ConflictException("Transition from " + current + " to " + target + " is not allowed.");
        }
    }
}
