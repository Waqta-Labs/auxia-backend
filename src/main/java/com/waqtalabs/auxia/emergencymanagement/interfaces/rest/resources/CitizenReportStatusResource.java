package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.application.queryservices.CitizenReportProgress.Stage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Progress of an SOS. It tells nothing about other reports or people.")
public record CitizenReportStatusResource(
    @Schema(example = "PRIORITIZED") Stage status,
    @Schema(
            description =
                "Urgency Score of the zone of the SOS, from 0 to 100, once it is PRIORITIZED,"
                    + " ACCEPTED or BRIGADE_ON_THE_WAY (A_05: \"Priorizado por IA · 94\"); null"
                    + " before",
            example = "94.0")
        Double score,
    @Schema(
            description =
                "Minutes until the brigade arrives, only with BRIGADE_ON_THE_WAY: a straight line"
                    + " from its base at 30 km/h; null without coordinates",
            example = "12")
        Integer etaMinutes,
    @Schema(
            description =
                "When an authority of the command center approved the plan of the zone that"
                    + " answers the SOS, with ACCEPTED and BRIGADE_ON_THE_WAY; null before",
            example = "2026-11-06T10:15:00")
        LocalDateTime acceptedAt) {}
