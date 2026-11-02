package com.waqtalabs.auxia.emergencymanagement.application.queries;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;

/** A plan of the organization of the user. */
public record GetDistributionPlanByIdQuery(
    DistributionPlanId planId, OrganizationId organizationId) {}
