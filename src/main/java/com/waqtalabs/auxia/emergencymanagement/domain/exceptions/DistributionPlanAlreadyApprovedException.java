package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** An approved plan cannot be approved again (US-12, scenario 2). */
public class DistributionPlanAlreadyApprovedException extends ConflictException {

  public DistributionPlanAlreadyApprovedException(DistributionPlanId id) {
    super("Distribution plan " + id.value() + " is already approved", id.value());
  }
}
