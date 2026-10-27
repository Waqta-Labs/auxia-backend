package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RestockRequest(
    @Schema(description = "Quantity received, in the unit of the resource", example = "120")
        @NotNull
        @Positive
        Integer quantity) {}
