package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanStatus;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** Only an approved plan can be executed. */
public class DistributionPlanNotApprovedException extends ConflictException {

  public DistributionPlanNotApprovedException(
      DistributionPlanId id, DistributionPlanStatus status) {
    super(
        "Distribution plan " + id.value() + " is " + status + " and not approved",
        id.value(),
        status);
  }
}
