package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectDistributionPlanRequest(
    @Schema(
            description = "Why the plan is rejected; it stays in the history of the zone (US-14)",
            example = "La vía de acceso está bloqueada")
        @NotBlank
        @Size(max = 1000)
        String reason) {}
