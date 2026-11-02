package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;

/** Closes the assignment of a plan once its delivery is confirmed. */
public record CompleteBrigadeAssignmentCommand(DistributionPlanId distributionPlanId) {}
