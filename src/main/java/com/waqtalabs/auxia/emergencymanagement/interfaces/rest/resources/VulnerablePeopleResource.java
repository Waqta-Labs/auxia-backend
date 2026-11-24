package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Vulnerable people that need help")
public record VulnerablePeopleResource(
    @Schema(example = "3") @NotNull @Min(0) @Max(500) Integer children,
    @Schema(example = "0") @NotNull @Min(0) @Max(500) Integer elderly,
    @Schema(example = "2") @NotNull @Min(0) @Max(500) Integer injured) {}
