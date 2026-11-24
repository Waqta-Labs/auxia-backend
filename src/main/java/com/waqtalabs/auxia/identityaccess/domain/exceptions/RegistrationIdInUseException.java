package com.waqtalabs.auxia.identityaccess.domain.exceptions;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.BrigadeRegistrationId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** The id of a request belongs to the phone that sent it, which proves it with its secret. */
public class RegistrationIdInUseException extends ConflictException {

  public RegistrationIdInUseException(BrigadeRegistrationId id) {
    super("Brigade registration " + id.value() + " belongs to another device", id.value());
  }
}
