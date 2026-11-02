package com.waqtalabs.auxia.shared.application.exceptions;

/**
 * A use case found that the user cannot run it now, for example because their role changed
 * after the token was issued (QAD-02). The API answers 403, like a missing role.
 */
public class ActionNotAllowedException extends RuntimeException {

  public ActionNotAllowedException(String reason) {
    super(reason);
  }
}
