package com.civicconnect.audit.persistence;

import com.civicconnect.audit.domain.RequestAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RequestAuditRepository extends JpaRepository<RequestAudit, UUID> {
    List<RequestAudit> findByRequestIdOrderByOccurredAtAsc(UUID requestId);
}
