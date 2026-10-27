package com.waqtalabs.auxia.identityaccess.domain.exceptions;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.EmailAddress;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** An email is unique across the platform, so sign-in does not need the organization. */
public class DuplicateEmailException extends ConflictException {

  public DuplicateEmailException(EmailAddress email) {
    super("A user with email " + email.value() + " already exists", email.value());
  }
}
