package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** {@code notes} is optional: what the next responsible should know about the zones. */
public record HandOverShiftRequest(
    @NotNull UUID incomingAuthorityId,
    @Schema(example = "Alto Perú sigue sin acceso por la carretera; coordinar con la brigada.")
        @Size(max = 2000)
        String notes) {}
