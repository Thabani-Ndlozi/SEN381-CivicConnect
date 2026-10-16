package com.civicconnect.management;

import com.civicconnect.request.domain.RequestStatus;
import com.civicconnect.request.domain.RequestCategory;
import com.civicconnect.audit.application.AuditView;
import com.civicconnect.request.application.RequestService;
import java.util.List;
import java.util.UUID;
import java.util.EnumMap;
import org.springframework.web.bind.annotation.PathVariable;
import com.civicconnect.request.persistence.ServiceRequestRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumMap;
import java.util.Map;

@RestController
@RequestMapping("/api/management")
public class ManagementController {
    private final ServiceRequestRepository repository;
    private final RequestService requestService;

    public ManagementController(ServiceRequestRepository repository, RequestService requestService) {
        this.repository = repository;
        this.requestService = requestService;
    }

    @GetMapping("/requests/{id}/history")
    public List<AuditView> requestHistory(@PathVariable UUID id) {
        return requestService.managementHistory(id);
    }

    // Live grouped view; overdue cannot be claimed until A-005 supplies a rule.
    @GetMapping("/breakdown")
    public Map<RequestCategory, Map<RequestStatus, Long>> breakdown() {
        Map<RequestCategory, Map<RequestStatus, Long>> result = new EnumMap<>(RequestCategory.class);
        for (RequestCategory category : RequestCategory.values()) {
            Map<RequestStatus, Long> byStatus = new EnumMap<>(RequestStatus.class);
            for (RequestStatus status : RequestStatus.values()) {
                byStatus.put(status, repository.countByCategoryAndStatus(category, status));
            }
            result.put(category, byStatus);
        }
        return result;
    }

    @GetMapping("/summary")
    public Map<RequestStatus, Long> summary() {
        Map<RequestStatus, Long> counts = new EnumMap<>(RequestStatus.class);
        for (RequestStatus status : RequestStatus.values()) {
            counts.put(status, repository.countByStatus(status));
        }
        return counts;
    }
}
