package com.waqtalabs.auxia.emergencymanagement.domain.events;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;

/** A report that was pending zone assignment now belongs to a zone. */
public record FieldReportAssignedToZone(FieldReportId fieldReportId, ZoneId zoneId)
    implements DomainEvent {}
