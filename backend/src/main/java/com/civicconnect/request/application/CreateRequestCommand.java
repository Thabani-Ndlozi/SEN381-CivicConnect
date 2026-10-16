package com.civicconnect.request.application;

import com.civicconnect.request.domain.RequestCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRequestCommand(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull RequestCategory category,
        @Size(max = 500) String location,
        @Pattern(regexp = "LOW|NORMAL|HIGH|URGENT") String priority,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
) {
    // Existing integrations that submit addresses without coordinates remain valid.
    public CreateRequestCommand(String title, String description, RequestCategory category) {
        this(title, description, category, null, null, null, null);
    }
    public CreateRequestCommand(String title, String description, RequestCategory category, String location, String priority) {
        this(title, description, category, location, priority, null, null);
    }
    @AssertTrue(message = "Latitude and longitude must both be provided with a location, or both omitted")
    public boolean isCoordinatesConsistent() {
        return (latitude == null && longitude == null) ||
            (latitude != null && longitude != null && location != null && !location.isBlank());
    }
}
