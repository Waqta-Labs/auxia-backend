package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** A resource appears once in a plan. */
public class DuplicateDistributionResourceException extends RuleViolationException {

  public DuplicateDistributionResourceException(DistributionPlanId id, ResourceId resourceId) {
    super(
        "Resource " + resourceId.value()
            + " appears more than once in distribution plan "
            + id.value(),
        resourceId.value(),
        id.value());
  }
}
