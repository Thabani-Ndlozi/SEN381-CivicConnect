package com.civicconnect.request.application;

import com.civicconnect.request.domain.RequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeStatusCommand(
        @NotNull RequestStatus targetStatus,
        @Size(max = 2000) String comment
) {}
