package com.waqtalabs.auxia.identityaccess.domain.exceptions;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.BrigadeRegistrationId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** A request is approved or rejected once. */
public class RegistrationAlreadyDecidedException extends ConflictException {

  public RegistrationAlreadyDecidedException(BrigadeRegistrationId id) {
    super("Brigade registration " + id.value() + " was already decided", id.value());
  }
}
