package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record ResourceQuantityRequest(
    @Schema(description = "Resource of the inventory of the organization") @NotNull UUID resourceId,
    @Schema(description = "New quantity; 0 takes the resource out of the plan", example = "200")
        @NotNull
        @PositiveOrZero
        Integer quantity) {}
