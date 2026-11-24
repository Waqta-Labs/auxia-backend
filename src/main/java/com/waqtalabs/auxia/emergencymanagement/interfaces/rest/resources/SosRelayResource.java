package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** How an SOS reached the phone that uploads it, by Mesh from the phone that sent it. */
public record SosRelayResource(
    @Schema(description = "Phones the SOS went through, the uploader included", example = "2")
        @NotNull
        @Min(1)
        @Max(3)
        Integer hops) {}
