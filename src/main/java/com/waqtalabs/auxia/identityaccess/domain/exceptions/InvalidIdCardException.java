package com.waqtalabs.auxia.identityaccess.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** The photo of the institutional card is a JPEG, PNG, WEBP or HEIC image of up to 10 MB. */
public class InvalidIdCardException extends RuleViolationException {

  public InvalidIdCardException(String reason) {
    super("Invalid id card: " + reason, reason);
  }
}
