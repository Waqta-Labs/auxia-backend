package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** A distribution plan reserves its resources only once. */
public class ReservationAlreadyExistsException extends ConflictException {

  public ReservationAlreadyExistsException(DistributionPlanId distributionPlanId) {
    super(
        "Distribution plan " + distributionPlanId.value() + " already has a reservation",
        distributionPlanId.value());
  }
}
