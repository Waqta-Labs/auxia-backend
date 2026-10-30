package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ManualPriorityRequest(
    @Schema(example = "Riesgo inminente de un nuevo huaico") @NotBlank String reason) {}
