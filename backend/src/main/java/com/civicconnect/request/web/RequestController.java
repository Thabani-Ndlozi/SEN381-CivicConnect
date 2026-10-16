package com.civicconnect.request.web;

import com.civicconnect.request.application.CreateRequestCommand;
import com.civicconnect.request.application.RequestService;
import com.civicconnect.request.application.RequestView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import com.civicconnect.audit.application.AuditView;

@RestController
@RequestMapping("/api/requests")
public class RequestController {
    private final RequestService service;

    public RequestController(RequestService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequestView create(Principal principal, @Valid @RequestBody CreateRequestCommand command) {
        return service.create(principal.getName(), command);
    }

    @GetMapping("/{id}/history")
    public List<AuditView> history(@PathVariable UUID id, Principal principal) {
        return service.requesterHistory(id, principal.getName());
    }

    @GetMapping("/mine")
    public List<RequestView> mine(Principal principal) {
        return service.myRequests(principal.getName());
    }
}
