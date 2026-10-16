package com.civicconnect.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class ReleaseController {
    @Value("${civicconnect.release-id:${CIVICCONNECT_RELEASE_ID:unversioned-local}}")
    private String releaseId;

    @GetMapping("/api/release")
    public Map<String, String> release() { return Map.of("release", releaseId); }
}
