package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeStatus;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** A brigade changes only between AVAILABLE and OFF_DUTY; it becomes ASSIGNED with a plan. */
public class InvalidBrigadeAvailabilityException extends RuleViolationException {

  public InvalidBrigadeAvailabilityException(BrigadeStatus status) {
    super("A brigade cannot be set to " + status + " by hand", status);
  }
}
