package com.civicconnect.request.domain;

import jakarta.persistence.*;
import com.civicconnect.shared.ConflictException;
import org.springframework.security.access.AccessDeniedException;
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

    @Column(length=500) private String location;
    @Column(length=20) private String priority;
    @Column(name = "latitude") private Double latitude;
    @Column(name = "longitude") private Double longitude;
    public String getLocation() { return location; }
    public String getPriority() { return priority; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public void setDetails(String location, String priority, Double latitude, Double longitude) {
        this.location = location;
        this.priority = priority;
        this.latitude = latitude;
        this.longitude = longitude;
    }
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
        if (staffId == null || staffId.isBlank() || ownerId != null || status != RequestStatus.SUBMITTED) {
            throw new ConflictException("Only an unassigned submitted request can be accepted.");
        }
        this.ownerId = staffId;
        if (this.status == RequestStatus.SUBMITTED) {
            this.status = RequestStatus.ACCEPTED;
        }
        this.updatedAt = now;
    }

    public void assertOwnedBy(String staffId) {
        if (ownerId == null || !ownerId.equals(staffId)) {
            throw new AccessDeniedException("Only the current owner may change this request.");
        }
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
