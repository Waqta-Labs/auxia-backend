package com.waqtalabs.auxia.identityaccess.domain.exceptions;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/**
 * An administrator cannot change their own role or status, so the organization keeps an
 * administrator.
 */
public class SelfManagementException extends RuleViolationException {

  public SelfManagementException(UserId id) {
    super("User " + id.value() + " cannot change their own role or status", id.value());
  }
}
