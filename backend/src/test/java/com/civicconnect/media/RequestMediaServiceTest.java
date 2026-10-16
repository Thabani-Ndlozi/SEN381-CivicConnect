package com.civicconnect.media;

import com.civicconnect.audit.application.AuditService;
import com.civicconnect.request.application.CreateRequestCommand;
import com.civicconnect.request.application.RequestService;
import com.civicconnect.request.application.RequestView;
import com.civicconnect.request.domain.RequestCategory;
import com.civicconnect.request.domain.ServiceRequest;
import com.civicconnect.request.persistence.ServiceRequestRepository;
import com.civicconnect.shared.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestMediaServiceTest {
    private static final byte[] SMALL_PNG = java.util.Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9WLRr5kAAAAASUVORK5CYII=");
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final RequestService requests = mock(RequestService.class);
    private final ServiceRequestRepository repo = mock(ServiceRequestRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final RequestMediaService service = new RequestMediaService(jdbc, requests, repo, audit);

    @Test void imageMustBeRealPngOrJpegNotArbitraryFile() {
        var fake = new MockMultipartFile("image", "unsafe.svg", "image/png", "<svg/>".getBytes());
        assertThrows(ResponseStatusException.class, () -> RequestMediaService.validateImage(fake));
    }
    @Test void imageLargerThanFiveMegabytesIsRejected() {
        var file = new MockMultipartFile("image", "large.png", "image/png", new byte[RequestMediaService.MAX_BYTES + 1]);
        assertThrows(ResponseStatusException.class, () -> RequestMediaService.validateImage(file));
    }
    @Test void unauthorizedRequesterCannotReadForeignRequestMedia() {
        var request = ServiceRequest.create("owner@example.com", "Broken gate", "Gate damaged", RequestCategory.FACILITY_FAULT, Instant.now());
        when(repo.findById(request.getId())).thenReturn(Optional.of(request));
        assertThrows(NotFoundException.class, () -> service.list(request.getId(), "someone-else@example.com", false));
        verifyNoInteractions(jdbc);
    }
    @Test void staffCannotPostCommentToRequestTheyDoNotOwn() {
        var request = ServiceRequest.create("owner@example.com", "Broken gate", "Gate damaged", RequestCategory.FACILITY_FAULT, Instant.now());
        request.assignTo("assigned-staff@example.com", Instant.now());
        when(repo.findById(request.getId())).thenReturn(Optional.of(request));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
            () -> service.addComment(request.getId(), "other-staff@example.com", false, "Will handle this", null));
        verifyNoInteractions(jdbc, audit);
    }
    @Test void invalidUploadDoesNotCreateRequestOrAudit() {
        var invalid = new MockMultipartFile("image", "bad.txt", "text/plain", "not an image".getBytes());
        var cmd = new CreateRequestCommand("Broken gate", "Gate damaged", RequestCategory.FACILITY_FAULT);
        assertThrows(ResponseStatusException.class,
            () -> service.createWithImage("owner@example.com", cmd, invalid));
        verifyNoInteractions(requests, jdbc, audit);
    }
}
