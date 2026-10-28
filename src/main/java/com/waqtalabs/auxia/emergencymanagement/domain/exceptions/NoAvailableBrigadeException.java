package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** An approved plan needs an available brigade with a leader to be delivered. */
public class NoAvailableBrigadeException extends RuleViolationException {

  public NoAvailableBrigadeException(DistributionPlanId planId) {
    super(
        "No brigade of the organization is available, with a leader, for plan " + planId.value(),
        planId.value());
  }
}
