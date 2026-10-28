package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationLine;
import java.util.List;

/** The stock held for a plan changes to these lines, for example after a modification. */
public record ChangeReservationCommand(
    DistributionPlanId distributionPlanId, List<ReservationLine> lines) {}
