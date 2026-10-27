package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

public class ReservationNotFoundException extends NotFoundException {

  public ReservationNotFoundException(DistributionPlanId distributionPlanId) {
    super(
        "Distribution plan " + distributionPlanId.value() + " has no active reservation",
        distributionPlanId.value());
  }
}
