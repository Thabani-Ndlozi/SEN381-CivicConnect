package com.civicconnect.request.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_requests")
public class ServiceRequest {
    @Id
    private UUID id;

    @Column(name = "requester_id", nullable = false, length = 120)
    private String requesterId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RequestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status;

    @Column(name = "owner_id", length = 120)
    private String ownerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ServiceRequest() {}

    private ServiceRequest(UUID id, String requesterId, String title, String description,
                           RequestCategory category, Instant now) {
        this.id = id;
        this.requesterId = requesterId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.status = RequestStatus.SUBMITTED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static ServiceRequest create(String requesterId, String title, String description,
                                        RequestCategory category, Instant now) {
        return new ServiceRequest(UUID.randomUUID(), requesterId, title, description, category, now);
    }

    public void assignTo(String staffId, Instant now) {
        this.ownerId = staffId;
        if (this.status == RequestStatus.SUBMITTED) {
            this.status = RequestStatus.ACCEPTED;
        }
        this.updatedAt = now;
    }

    public void changeStatus(RequestStatus targetStatus, Instant now) {
        this.status = targetStatus;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getRequesterId() { return requesterId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public RequestCategory getCategory() { return category; }
    public RequestStatus getStatus() { return status; }
    public String getOwnerId() { return ownerId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
