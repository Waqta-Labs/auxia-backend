package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

public class AssignmentNotFoundException extends NotFoundException {

  public AssignmentNotFoundException(BrigadeId brigadeId, DistributionPlanId planId) {
    super(
        "Brigade "
            + brigadeId.value()
            + " has no assignment in progress for plan "
            + planId.value(),
        brigadeId.value(),
        planId.value());
  }
}
