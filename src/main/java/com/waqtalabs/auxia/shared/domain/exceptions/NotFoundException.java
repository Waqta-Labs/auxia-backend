package com.waqtalabs.auxia.shared.domain.exceptions;

/** The requested aggregate does not exist. Answered with 404. */
public abstract non-sealed class NotFoundException extends DomainException {

  protected NotFoundException(String message, Object... arguments) {
    super(message, arguments);
  }
}
