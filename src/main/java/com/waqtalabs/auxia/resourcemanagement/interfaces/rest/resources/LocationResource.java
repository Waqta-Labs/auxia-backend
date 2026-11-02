package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record LocationResource(
    @Schema(example = "-11.9436") @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
    @Schema(example = "-76.7078") @NotNull @DecimalMin("-180") @DecimalMax("180")
        Double longitude) {}
