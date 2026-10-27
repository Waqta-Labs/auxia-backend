package com.waqtalabs.auxia.identityaccess.domain.exceptions;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.AccountStatus;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** An administrator only enables or disables accounts; they lock after failed sign-ins. */
public class InvalidAccountStatusException extends RuleViolationException {

  public InvalidAccountStatusException(AccountStatus status) {
    super("An account cannot be set to " + status + " by hand", status);
  }
}
