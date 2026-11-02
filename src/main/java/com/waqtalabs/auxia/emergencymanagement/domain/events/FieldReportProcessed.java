package com.waqtalabs.auxia.emergencymanagement.domain.events;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;

/**
 * The NLP structured a field report.
 *
 * @param zoneId zone of the report, or {@code null} while it is pending zone assignment
 */
public record FieldReportProcessed(FieldReportId fieldReportId, ZoneId zoneId)
    implements DomainEvent {}
