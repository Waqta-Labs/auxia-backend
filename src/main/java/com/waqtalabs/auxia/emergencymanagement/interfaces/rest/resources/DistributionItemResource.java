package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record DistributionItemResource(
    UUID resourceId,
    @Schema(
            description =
                "Quantity of the original recommendation; null for a resource the authority added")
        Integer recommendedQuantity,
    @Schema(description = "Quantity to deliver now; 0 takes the resource out of the plan")
        int quantity) {}
