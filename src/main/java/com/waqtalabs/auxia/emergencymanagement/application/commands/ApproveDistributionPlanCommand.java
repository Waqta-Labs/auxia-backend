package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;

/** An authority approves a proposed plan (US-12). */
public record ApproveDistributionPlanCommand(
    DistributionPlanId planId, AuthorityId authorityId, OrganizationId organizationId) {}
