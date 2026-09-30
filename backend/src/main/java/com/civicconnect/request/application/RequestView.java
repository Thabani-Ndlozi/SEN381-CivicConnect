package com.civicconnect.request.application;

import com.civicconnect.request.domain.RequestCategory;
import com.civicconnect.request.domain.RequestStatus;
import com.civicconnect.request.domain.ServiceRequest;

import java.time.Instant;
import java.util.UUID;

public record RequestView(
        UUID id,
        String requesterId,
        String title,
        String description,
        RequestCategory category,
        RequestStatus status,
        String ownerId,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static RequestView from(ServiceRequest request) {
        return new RequestView(request.getId(), request.getRequesterId(), request.getTitle(),
                request.getDescription(), request.getCategory(), request.getStatus(),
                request.getOwnerId(), request.getCreatedAt(), request.getUpdatedAt(), request.getVersion());
    }
}
