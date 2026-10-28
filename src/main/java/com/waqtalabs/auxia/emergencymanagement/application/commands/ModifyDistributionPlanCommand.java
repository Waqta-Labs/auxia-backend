package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceQuantity;
import java.util.List;

/** An authority changes quantities of a proposed plan, with a reason (US-13, US-14). */
public record ModifyDistributionPlanCommand(
    DistributionPlanId planId,
    AuthorityId authorityId,
    OrganizationId organizationId,
    List<ResourceQuantity> changes,
    String reason) {}
