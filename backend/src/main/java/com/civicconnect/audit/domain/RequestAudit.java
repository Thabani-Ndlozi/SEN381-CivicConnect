package com.civicconnect.audit.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_audit")
public class RequestAudit {
    @Id
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(nullable = false, length = 120)
    private String actor;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", length = 30)
    private String toStatus;

    @Column(length = 2000)
    private String comment;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected RequestAudit() {}

    public RequestAudit(UUID requestId, String actor, String action, String fromStatus,
                        String toStatus, String comment, Instant occurredAt) {
        this.id = UUID.randomUUID();
        this.requestId = requestId;
        this.actor = actor;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.comment = comment;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getRequestId() { return requestId; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getFromStatus() { return fromStatus; }
    public String getToStatus() { return toStatus; }
    public String getComment() { return comment; }
    public Instant getOccurredAt() { return occurredAt; }
}
