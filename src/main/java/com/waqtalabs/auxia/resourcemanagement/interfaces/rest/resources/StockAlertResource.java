package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockAlertStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/** A resource that fell below its minimum stock (US-10). */
public record StockAlertResource(
    UUID id,
    UUID resourceId,
    @Schema(description = "Name of the resource when the alert was raised") String resourceName,
    StockAlertStatus status,
    @Schema(description = "Available quantity when the alert was raised") int availableQuantity,
    int minimumStock,
    LocalDateTime raisedAt,
    @Schema(description = "Null while the alert is open") LocalDateTime resolvedAt) {}
