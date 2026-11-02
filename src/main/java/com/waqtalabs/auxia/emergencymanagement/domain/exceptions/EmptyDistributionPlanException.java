package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** A modification cannot leave a plan without resources; reject it instead. */
public class EmptyDistributionPlanException extends RuleViolationException {

  public EmptyDistributionPlanException(DistributionPlanId id) {
    super("Distribution plan " + id.value() + " would have no resource to distribute", id.value());
  }
}
