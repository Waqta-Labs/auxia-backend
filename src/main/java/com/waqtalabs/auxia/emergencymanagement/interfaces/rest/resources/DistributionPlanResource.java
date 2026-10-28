package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Coverage;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DistributionPlanResource(
    UUID id,
    UUID emergencyId,
    UUID zoneId,
    @Schema(description = "Organization whose inventory the plan reserves") UUID organizationId,
    DistributionPlanStatus status,
    @Schema(description = "How much of the need of the zone the recommendation covers")
        Coverage coverage,
    @Schema(description = "Justification of the recommendation") String justification,
    String modelVersion,
    LocalDateTime createdAt,
    List<DistributionItemResource> items,
    @Schema(description = "Null if the quantities were never changed")
        PlanModificationResource modification,
    @Schema(description = "Null while the plan is proposed") PlanDecisionResource decision,
    @Schema(description = "Only for a rejected plan") String rejectedReason) {}
