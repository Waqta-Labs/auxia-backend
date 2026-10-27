package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** A count of the population impact is greater than the count that should contain it. */
public class InconsistentPopulationImpactException extends RuleViolationException {

  public InconsistentPopulationImpactException(String count, String limit) {
    super(count + " cannot be greater than " + limit, count, limit);
  }
}
