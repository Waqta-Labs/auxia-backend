package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** Only an active FIELD_BRIGADE account of the same organization is linked to a member. */
public class BrigadistAccountRequiredException extends RuleViolationException {

  public BrigadistAccountRequiredException(UserId userId) {
    super(
        "User " + userId.value() + " is not an active brigadist of the organization",
        userId.value());
  }
}
