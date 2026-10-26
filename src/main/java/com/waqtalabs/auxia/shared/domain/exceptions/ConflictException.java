package com.waqtalabs.auxia.shared.domain.exceptions;

/** The operation is not allowed in the current state of the aggregate. Answered with 409. */
public abstract class ConflictException extends DomainException {

  protected ConflictException(String message, Object... arguments) {
    super(message, arguments);
  }
}
