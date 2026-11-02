package com.waqtalabs.auxia.emergencymanagement.application.queries;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;

/**
 * @param pendingZoneAssignmentOnly {@code true} to get only the reports that still have no zone
 */
public record GetFieldReportsByEmergencyIdQuery(
    EmergencyId emergencyId, boolean pendingZoneAssignmentOnly) {}
