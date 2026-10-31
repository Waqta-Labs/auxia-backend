package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/** Who corrected the indicators of a report, when and why, and what the NLP had extracted. */
public record FieldReportCorrectionResource(
    @Schema(description = "Indicators as the NLP extracted them") StructuredFieldDataResource
        originalData,
    UUID correctedBy,
    LocalDateTime correctedAt,
    String reason) {}
