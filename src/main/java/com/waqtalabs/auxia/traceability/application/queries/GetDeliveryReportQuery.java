package com.waqtalabs.auxia.traceability.application.queries;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Deliveries of the organization to export. {@code zoneId}, {@code from} and {@code to} are
 * optional; both days of the period are included.
 */
public record GetDeliveryReportQuery(
    OrganizationId organizationId, ZoneId zoneId, LocalDate from, LocalDate to, Locale locale) {}
