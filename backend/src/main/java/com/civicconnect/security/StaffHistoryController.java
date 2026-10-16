package com.civicconnect.security;
import org.springframework.web.bind.annotation.*;
import com.civicconnect.request.application.RequestService;
import com.civicconnect.audit.application.AuditView;
import java.util.*;
@RestController
public class StaffHistoryController {
 private final RequestService service;
 public StaffHistoryController(RequestService service) { this.service=service; }
 @GetMapping("/api/staff/requests/{id}/history") public List<AuditView> history(@PathVariable UUID id) { return service.managementHistory(id); }
}
