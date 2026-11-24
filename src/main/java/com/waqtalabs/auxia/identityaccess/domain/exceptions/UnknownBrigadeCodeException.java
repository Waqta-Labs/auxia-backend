package com.waqtalabs.auxia.identityaccess.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

/** A brigadist registers with the code of an existing brigade. */
public class UnknownBrigadeCodeException extends NotFoundException {

  public UnknownBrigadeCodeException(String code) {
    super("No brigade has the code " + code, code);
  }
}
