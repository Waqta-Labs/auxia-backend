package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Resources not listed keep their quantity; a resource not in the plan is added. */
public record ModifyDistributionPlanRequest(
    @NotEmpty @Valid List<ResourceQuantityRequest> items,
    @Schema(
            description = "Why the recommendation changes, for a later audit (US-14)",
            example = "La zona recibió víveres de otra ONG")
        @NotBlank
        @Size(max = 1000)
        String reason) {}
