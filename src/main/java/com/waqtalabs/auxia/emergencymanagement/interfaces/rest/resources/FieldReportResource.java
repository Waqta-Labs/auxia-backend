package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import java.time.LocalDateTime;
import java.util.UUID;

/** {@code zoneId} is {@code null} while the report is pending zone assignment. */
public record FieldReportResource(
    UUID id,
    UUID emergencyId,
    UUID zoneId,
    String text,
    ReportSource source,
    LocalDateTime reportedAt,
    boolean pendingZoneAssignment) {}
