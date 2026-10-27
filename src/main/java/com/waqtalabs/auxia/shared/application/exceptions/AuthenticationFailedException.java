package com.waqtalabs.auxia.shared.application.exceptions;

/**
 * The credentials of a request are not valid. The API answers 401 with one generic message that
 * never says which part failed (US-21).
 */
public class AuthenticationFailedException extends RuntimeException {

  public AuthenticationFailedException(String reason) {
    super(reason);
  }
}
