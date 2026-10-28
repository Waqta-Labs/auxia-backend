package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;

/** An authority rejects a proposed plan with a reason (US-14). */
public record RejectDistributionPlanCommand(
    DistributionPlanId planId,
    AuthorityId authorityId,
    OrganizationId organizationId,
    String reason) {}
