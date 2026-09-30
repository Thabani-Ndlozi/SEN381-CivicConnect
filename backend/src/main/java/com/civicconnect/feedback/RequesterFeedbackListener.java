package com.civicconnect.feedback;

import com.civicconnect.request.application.event.RequestStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observer-style secondary reaction. It runs only after the correctness-critical
 * request + audit transaction commits. A real notification provider is deliberately
 * not introduced because external notifications are still deferred scope.
 */
@Component
public class RequesterFeedbackListener {
    private static final Logger log = LoggerFactory.getLogger(RequesterFeedbackListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(RequestStatusChangedEvent event) {
        log.info("Requester feedback event: request={} requester={} {} -> {}",
                event.requestId(), event.requesterId(), event.fromStatus(), event.toStatus());
    }
}
