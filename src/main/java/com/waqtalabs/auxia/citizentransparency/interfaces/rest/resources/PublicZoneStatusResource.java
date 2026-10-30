package com.waqtalabs.auxia.citizentransparency.interfaces.rest.resources;

import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.PublicStage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The stage of a zone and its delivery history (US-24, US-25). No personal data, no score, no
 * position in the ranking and no delivered resources: crossed between zones they would reveal
 * the internal prioritization (R-04).
 */
public record PublicZoneStatusResource(
    UUID zoneId,
    UUID emergencyId,
    String zoneName,
    PublicStage stage,
    @Schema(description = "Deliveries completed in the zone") int deliveriesCompleted,
    @Schema(description = "Null until a delivery is completed") LocalDateTime lastDeliveryAt,
    @Schema(description = "Null until a delivery is completed")
        PublicDeliveryVerification lastDeliveryVerification,
    @Schema(
            description =
                "Public verification of the last delivery, when it is verified; its code is the"
                    + " delivery identifier (US-26)")
        String verificationUrl) {}
