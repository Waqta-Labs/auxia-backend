package com.waqtalabs.auxia.traceability.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The {@code delivery} part of the registration. The evidence files travel in the {@code
 * evidences} parts of the same request. It has no field for names, documents or any other data
 * of the people who received the aid.
 */
public record RegisterDeliveryRequest(
    @Schema(
            description = "When the brigade delivered, in the field; to the second",
            example = "2026-03-15T10:30:00")
        @NotNull
        LocalDateTime deliveredAt,
    @Schema(description = "What was delivered; it can be less than planned")
        @NotEmpty
        List<@Valid DeliveredItemRequest> items,
    @Schema(description = "Where the evidence was captured; optional") @Valid
        GeoPointResource location) {}
