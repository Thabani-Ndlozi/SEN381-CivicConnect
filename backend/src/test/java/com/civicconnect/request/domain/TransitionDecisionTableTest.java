package com.civicconnect.request.domain;

import com.civicconnect.shared.ConflictException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/** TEST UT-07--UT-13: decision table and state transition technique; A-005 policy provisional. */
class TransitionDecisionTableTest {
    private final RequestStatusTransitionPolicy policy = new RequestStatusTransitionPolicy();
    @ParameterizedTest(name="{0} -> {1}")
    @CsvSource({
      "SUBMITTED,ACCEPTED,true", "SUBMITTED,CLOSED,false",
      "ACCEPTED,IN_PROGRESS,true", "ACCEPTED,CLOSED,false",
      "IN_PROGRESS,RESOLVED,true", "RESOLVED,IN_PROGRESS,true",
      "RESOLVED,CLOSED,true", "CLOSED,IN_PROGRESS,false",
      "REJECTED,ACCEPTED,false"
    })
    void transitionTable(RequestStatus current, RequestStatus target, boolean permitted) {
        if (permitted) assertDoesNotThrow(() -> policy.validate(current, target));
        else assertThrows(ConflictException.class, () -> policy.validate(current, target));
    }
}
