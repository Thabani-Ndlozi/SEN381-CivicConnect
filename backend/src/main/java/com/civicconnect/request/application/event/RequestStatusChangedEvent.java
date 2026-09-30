package com.civicconnect.request.application.event;

import com.civicconnect.request.domain.RequestStatus;
import java.time.Instant;
import java.util.UUID;

public record RequestStatusChangedEvent(
        UUID requestId,
        String requesterId,
        RequestStatus fromStatus,
        RequestStatus toStatus,
        Instant occurredAt
) {}
