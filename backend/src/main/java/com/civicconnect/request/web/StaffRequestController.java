package com.civicconnect.request.web;

import com.civicconnect.request.application.ChangeStatusCommand;
import com.civicconnect.request.application.RequestService;
import com.civicconnect.request.application.RequestView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/staff/requests")
public class StaffRequestController {
    private final RequestService service;

    public StaffRequestController(RequestService service) {
        this.service = service;
    }

    @GetMapping
    public List<RequestView> all() {
        return service.allRequests();
    }

    @PostMapping("/{id}/accept")
    public RequestView accept(@PathVariable UUID id, Principal principal) {
        return service.acceptOwnership(id, principal.getName());
    }

    @PostMapping("/{id}/status")
    public RequestView changeStatus(@PathVariable UUID id,
                                    Principal principal,
                                    @Valid @RequestBody ChangeStatusCommand command) {
        return service.changeStatus(id, principal.getName(), command);
    }
}
