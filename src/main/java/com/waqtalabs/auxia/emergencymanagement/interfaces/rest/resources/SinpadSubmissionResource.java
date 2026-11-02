package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** What SINPAD answered to the field reports of an emergency (US-33). */
public record SinpadSubmissionResource(
    @Schema(
            description = "Number of the emergency in SINPAD, the same in every submission",
            example = "SINPAD-2026-000001")
        String registrationCode,
    @Schema(description = "Field reports SINPAD received") int reportsReceived,
    @Schema(description = "When SINPAD received them") LocalDateTime receivedAt) {}
