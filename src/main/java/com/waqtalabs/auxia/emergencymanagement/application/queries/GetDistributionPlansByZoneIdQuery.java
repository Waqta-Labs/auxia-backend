package com.waqtalabs.auxia.emergencymanagement.application.queries;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;

/** History of the plans of a zone, with their decisions and justifications (US-14). */
public record GetDistributionPlansByZoneIdQuery(
    ZoneId zoneId, OrganizationId organizationId) {}
