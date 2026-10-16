package com.civicconnect.audit.application;

import com.civicconnect.audit.domain.RequestAudit;
import com.civicconnect.audit.persistence.RequestAuditRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Service
public class AuditService {
    private final RequestAuditRepository repository;

    public AuditService(RequestAuditRepository repository) {
        this.repository = repository;
    }

    public List<AuditView> historyForRequester(UUID requestId) {
        return repository.findByRequestIdOrderByOccurredAtAsc(requestId)
                .stream().map(AuditView::requester).toList();
    }

    public List<AuditView> historyForManagement(UUID requestId) {
        return repository.findByRequestIdOrderByOccurredAtAsc(requestId)
                .stream().map(AuditView::management).toList();
    }

    public void record(UUID requestId, String actor, String action, String fromStatus,
                       String toStatus, String comment, Instant occurredAt) {
        repository.save(new RequestAudit(requestId, actor, action, fromStatus, toStatus, comment, occurredAt));
    }
}
