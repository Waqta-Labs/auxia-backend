package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;

/** The authority assigns the zone of a report that is pending zone assignment. */
public record AssignFieldReportZoneCommand(FieldReportId fieldReportId, ZoneId zoneId) {}
