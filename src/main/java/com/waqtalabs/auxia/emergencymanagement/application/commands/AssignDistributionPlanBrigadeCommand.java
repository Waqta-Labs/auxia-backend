package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import java.util.Optional;

/**
 * Gets a brigade for an approved plan.
 *
 * @param requestedBy the authority who asks for it again; empty when it runs right after the
 *     approval
 */
public record AssignDistributionPlanBrigadeCommand(
    DistributionPlanId planId, OrganizationId organizationId, Optional<AuthorityId> requestedBy) {}
