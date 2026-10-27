package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeAvailabilityRequest(
    @Schema(description = "AVAILABLE or OFF_DUTY", example = "OFF_DUTY") @NotNull
        BrigadeStatus status) {}
