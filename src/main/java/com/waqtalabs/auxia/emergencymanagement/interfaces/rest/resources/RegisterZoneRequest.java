package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterZoneRequest(
    @Schema(example = "Sector Alto Perú") @NotBlank @Size(max = 150) String name,
    @Schema(description = "Optional") @Valid GeolocationResource geolocation,
    @NotNull @Valid PopulationImpactResource populationImpact,
    @Schema(example = "SEVERE") @NotNull DamageLevel damageLevel) {}
