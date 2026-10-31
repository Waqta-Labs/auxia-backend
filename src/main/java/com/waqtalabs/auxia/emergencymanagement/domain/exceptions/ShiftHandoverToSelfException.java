package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** A shift is handed over to another authority, never to the same one (US-23). */
public class ShiftHandoverToSelfException extends RuleViolationException {

  public ShiftHandoverToSelfException(AuthorityId authority) {
    super(
        "Authority " + authority.value() + " cannot hand over the shift to itself",
        authority.value());
  }
}
