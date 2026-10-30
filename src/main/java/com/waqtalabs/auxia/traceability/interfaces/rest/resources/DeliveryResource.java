package com.waqtalabs.auxia.traceability.interfaces.rest.resources;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A delivery. Its identifier is also the public code to verify it.
 *
 * @param registeredBy null, like the dates, while the delivery is {@code INITIATED}
 * @param integrity null while the delivery is {@code INITIATED}
 */
public record DeliveryResource(
    UUID id,
    UUID distributionPlanId,
    UUID zoneId,
    UUID brigadeId,
    DeliveryStatus status,
    UUID registeredBy,
    @Schema(description = "When the brigade delivered") LocalDateTime deliveredAt,
    @Schema(description = "When the backend received the registration") LocalDateTime receivedAt,
    List<DeliveryItemResource> items,
    List<EvidenceResource> evidences,
    IntegrityResource integrity) {}
