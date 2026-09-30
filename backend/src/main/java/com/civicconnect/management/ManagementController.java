package com.civicconnect.management;

import com.civicconnect.request.domain.RequestStatus;
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

    public ManagementController(ServiceRequestRepository repository) {
        this.repository = repository;
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
