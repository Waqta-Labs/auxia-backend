package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;

/** The stock held for a plan becomes available again, for example when it is rejected. */
public record ReleaseReservationCommand(DistributionPlanId distributionPlanId) {}
