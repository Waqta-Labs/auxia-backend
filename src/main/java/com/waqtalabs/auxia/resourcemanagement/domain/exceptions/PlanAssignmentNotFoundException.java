package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

/** No brigade was ever assigned to the distribution plan. */
public class PlanAssignmentNotFoundException extends NotFoundException {

  public PlanAssignmentNotFoundException(DistributionPlanId planId) {
    super("No brigade is assigned to plan " + planId.value(), planId.value());
  }
}
