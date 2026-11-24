package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** An account is a member of one brigade at most, so its missions are those of that brigade. */
public class AccountInAnotherBrigadeException extends ConflictException {

  public AccountInAnotherBrigadeException(UserId userId) {
    super("User " + userId.value() + " is already a member of a brigade", userId.value());
  }
}
