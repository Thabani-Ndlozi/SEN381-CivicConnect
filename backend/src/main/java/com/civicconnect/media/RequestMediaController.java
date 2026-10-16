package com.civicconnect.media;

import com.civicconnect.request.application.CreateRequestCommand;
import com.civicconnect.request.application.ChangeStatusCommand;
import com.civicconnect.request.application.RequestView;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
public class RequestMediaController {
    private final RequestMediaService media;
    public RequestMediaController(RequestMediaService media) { this.media = media; }

    @PostMapping(value="/api/requests/with-image", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RequestView create(@RequestPart("request") @Valid CreateRequestCommand command,
                              @RequestPart(value="image",required=false) MultipartFile image, Principal principal) {
        return media.createWithImage(principal.getName(), command, image);
    }

    @GetMapping("/api/requests/{id}/media")
    public List<RequestMediaService.MediaView> requesterMedia(@PathVariable UUID id, Principal principal) {
        return media.list(id, principal.getName(), false);
    }
    @GetMapping("/api/staff/requests/{id}/media")
    public List<RequestMediaService.MediaView> staffMedia(@PathVariable UUID id, Principal principal) {
        return media.list(id, principal.getName(), true);
    }

    @GetMapping("/api/requests/{id}/media/{mediaId}/image")
    public ResponseEntity<byte[]> requesterImage(@PathVariable UUID id, @PathVariable UUID mediaId, Principal principal) {
        return send(media.image(id, mediaId, principal.getName(), false));
    }
    @GetMapping("/api/staff/requests/{id}/media/{mediaId}/image")
    public ResponseEntity<byte[]> staffImage(@PathVariable UUID id, @PathVariable UUID mediaId, Principal principal) {
        return send(media.image(id, mediaId, principal.getName(), true));
    }

    @PostMapping(value="/api/staff/requests/{id}/status-with-image", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public RequestView statusWithImage(@PathVariable UUID id, Principal principal,
                            @RequestPart("status") @Valid ChangeStatusCommand command,
                            @RequestPart("image") MultipartFile image) {
        return media.changeStatusWithImage(id, principal.getName(), command, image);
    }

    @PostMapping(value="/api/staff/requests/{id}/comments", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RequestMediaService.MediaView comment(@PathVariable UUID id, Principal principal,
                           @RequestParam("comment") String comment,
                           @RequestPart(value="image",required=false) MultipartFile image,
                           org.springframework.security.core.Authentication authentication) {
        boolean manager = authentication.getAuthorities().stream().anyMatch(a -> (a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_SUPERUSER")));
        return media.addComment(id, principal.getName(), manager, comment, image);
    }

    private ResponseEntity<byte[]> send(RequestMediaService.ImageFile image) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(image.bytes());
    }
}
