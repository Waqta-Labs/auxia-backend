package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** A manual priority is registered with its justification (US-07, scenario 1). */
public class ManualPriorityReasonRequiredException extends RuleViolationException {

  public ManualPriorityReasonRequiredException(ZoneId id) {
    super("The manual priority of zone " + id.value() + " needs a justification", id.value());
  }
}
