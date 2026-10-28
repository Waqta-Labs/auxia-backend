package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

/** The plan does not exist or belongs to another organization. */
public class DistributionPlanNotFoundException extends NotFoundException {

  public DistributionPlanNotFoundException(DistributionPlanId id) {
    super("Distribution plan " + id.value() + " does not exist", id.value());
  }
}
