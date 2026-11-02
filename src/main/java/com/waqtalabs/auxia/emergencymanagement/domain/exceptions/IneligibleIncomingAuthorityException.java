package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/**
 * The zones of a shift pass only to an active authority of the same organization (US-23). Another
 * user, or one of another organization, is answered the same way, without saying which it is.
 */
public class IneligibleIncomingAuthorityException extends RuleViolationException {

  public IneligibleIncomingAuthorityException(AuthorityId authority) {
    super(
        "User " + authority.value() + " is not an active authority of the organization",
        authority.value());
  }
}
