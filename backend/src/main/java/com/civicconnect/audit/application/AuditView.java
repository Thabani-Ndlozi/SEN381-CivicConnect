package com.civicconnect.audit.application;

import com.civicconnect.audit.domain.RequestAudit;
import java.time.Instant;
import java.util.UUID;

// Audit reads are projected; requester views omit internal staff identifiers.
public record AuditView(UUID requestId, String action, String fromStatus,
                        String toStatus, String comment, Instant occurredAt, String actor) {
    public static AuditView requester(RequestAudit row) {
        return new AuditView(row.getRequestId(), row.getAction(), row.getFromStatus(),
                row.getToStatus(), row.getComment(), row.getOccurredAt(), null);
    }
    public static AuditView management(RequestAudit row) {
        return new AuditView(row.getRequestId(), row.getAction(), row.getFromStatus(),
                row.getToStatus(), row.getComment(), row.getOccurredAt(), row.getActor());
    }
}
