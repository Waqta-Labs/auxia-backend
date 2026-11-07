package com.waqtalabs.auxia.identityaccess.domain.exceptions;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.BrigadeRegistrationId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

/** The request does not exist, or its secret is not the one of the phone. */
public class BrigadeRegistrationNotFoundException extends NotFoundException {

  public BrigadeRegistrationNotFoundException(BrigadeRegistrationId id) {
    super("Brigade registration " + id.value() + " does not exist", id.value());
  }
}
