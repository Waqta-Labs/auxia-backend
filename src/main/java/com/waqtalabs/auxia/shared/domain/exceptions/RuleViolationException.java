package com.waqtalabs.auxia.shared.domain.exceptions;

/** The data is well formed but breaks a business rule. Answered with 422. */
public abstract non-sealed class RuleViolationException extends DomainException {

  protected RuleViolationException(String message, Object... arguments) {
    super(message, arguments);
  }
}
