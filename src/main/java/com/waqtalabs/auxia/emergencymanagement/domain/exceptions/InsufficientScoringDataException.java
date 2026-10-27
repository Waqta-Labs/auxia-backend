package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/**
 * The urgency score of a zone is not available because its population is unknown (US-04,
 * scenario 2).
 */
public class InsufficientScoringDataException extends RuleViolationException {

  public InsufficientScoringDataException(ZoneId id) {
    super("Zone " + id.value() + " has no population to calculate its urgency score", id.value());
  }
}
