package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanStatus;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** Only a proposed plan can be modified, approved or rejected. */
public class DistributionPlanNotPendingException extends ConflictException {

  public DistributionPlanNotPendingException(DistributionPlanId id, DistributionPlanStatus status) {
    super(
        "Distribution plan " + id.value() + " is " + status + " and no longer proposed",
        id.value(),
        status);
  }
}
