package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.application.queryservices.CitizenReportProgress.Stage;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Progress of an SOS. It tells nothing about other reports or people.")
public record CitizenReportStatusResource(
    @Schema(example = "PRIORITIZED") Stage status,
    @Schema(
            description =
                "Always null: the urgency score is an internal variable of the prioritization,"
                    + " which the public API does not reveal (TS-C02, R-04)")
        Double score,
    @Schema(description = "Minutes until the brigade arrives; null while it cannot be estimated")
        Integer etaMinutes) {}
