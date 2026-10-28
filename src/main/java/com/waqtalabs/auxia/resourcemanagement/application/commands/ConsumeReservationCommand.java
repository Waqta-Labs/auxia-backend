package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationLine;
import java.util.List;

/**
 * The delivery of a plan took its stock out of the warehouse.
 *
 * @param delivered what was really delivered, which can be less than reserved
 */
public record ConsumeReservationCommand(
    DistributionPlanId distributionPlanId, List<ReservationLine> delivered) {}
