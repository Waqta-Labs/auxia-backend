package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** Each distribution plan is delivered by one brigade. */
public class DistributionPlanAlreadyAssignedException extends ConflictException {

  public DistributionPlanAlreadyAssignedException(DistributionPlanId planId, BrigadeId brigadeId) {
    super(
        "Plan " + planId.value() + " is already assigned to brigade " + brigadeId.value(),
        planId.value(),
        brigadeId.value());
  }
}
