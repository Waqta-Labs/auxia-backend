package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** A brigade needs a member with the LEADER role to be assigned and while it is assigned. */
public class BrigadeWithoutLeaderException extends RuleViolationException {

  public BrigadeWithoutLeaderException(BrigadeId id) {
    super("Brigade " + id.value() + " has no leader", id.value());
  }
}
