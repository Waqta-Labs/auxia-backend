package com.waqtalabs.auxia.emergencymanagement.application.queries;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanStatus;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;

/** Plans of an emergency, optionally only those in one status. */
public record GetDistributionPlansByEmergencyIdQuery(
    EmergencyId emergencyId, OrganizationId organizationId, DistributionPlanStatus status) {}
