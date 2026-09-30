package com.civicconnect.request.persistence;

import com.civicconnect.request.domain.RequestCategory;
import com.civicconnect.request.domain.RequestStatus;
import com.civicconnect.request.domain.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {
    List<ServiceRequest> findByRequesterIdOrderByCreatedAtDesc(String requesterId);
    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
    List<ServiceRequest> findByCategoryOrderByCreatedAtDesc(RequestCategory category);
    long countByStatus(RequestStatus status);
}
