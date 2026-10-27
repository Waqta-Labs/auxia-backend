package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.shared.interfaces.rest.validation.NonZero;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdjustStockRequest(
    @Schema(description = "Positive to add, negative to remove; never 0", example = "-30")
        @NotNull
        @NonZero
        Integer quantityChange,
    @Schema(example = "Botellas dañadas en el traslado") @NotBlank @Size(max = 300)
        String reason) {}
