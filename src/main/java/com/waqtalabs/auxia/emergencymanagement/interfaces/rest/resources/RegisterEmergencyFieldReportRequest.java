package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Field report of an emergency, with or without its zone. */
public record RegisterEmergencyFieldReportRequest(
    @Schema(
            description = "Free text of the report, in Spanish",
            example = "Derrumbe de viviendas en el sector Alto Perú. Hay 35 familias afectadas.")
        @NotBlank
        @Size(max = 5000)
        String text,
    @Schema(example = "VOLUNTEER") @NotNull ReportSource source,
    @Schema(
            description =
                "Zone of the emergency. Without it, the zone is identified from the text when"
                    + " exactly one zone name appears; otherwise the report stays pending zone"
                    + " assignment.")
        UUID zoneId) {}
