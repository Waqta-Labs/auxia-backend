package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceCategory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UnitOfMeasure;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
    description =
        "A resource of the inventory. available = onHand - reserved: the reserved quantity is"
            + " held for distribution plans and cannot be distributed again (US-09).")
public record StockItemResource(
    UUID resourceId,
    String name,
    ResourceCategory category,
    UnitOfMeasure unit,
    int onHand,
    int reserved,
    int available,
    int minimumStock,
    @Schema(description = "available is below minimumStock") boolean belowMinimum) {}
