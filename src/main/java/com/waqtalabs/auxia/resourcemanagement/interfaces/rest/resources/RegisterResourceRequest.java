package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceCategory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UnitOfMeasure;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RegisterResourceRequest(
    @Schema(example = "Agua 2.5 L") @NotBlank @Size(max = 150) String name,
    @Schema(example = "WATER") @NotNull ResourceCategory category,
    @Schema(example = "UNIT") @NotNull UnitOfMeasure unit,
    @Schema(
            description = "Available quantity below which it needs restocking; 0 if missing",
            example = "50")
        @PositiveOrZero
        Integer minimumStock) {}
