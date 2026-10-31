package com.waqtalabs.auxia.shared.domain.exceptions;

import java.util.Map;

/**
 * Base of the errors raised by the domain of any bounded context.
 *
 * <p>The message is the English description for logs. The REST layer looks up the localized
 * detail by the exception class and fills it with {@link #arguments()}.
 */
public abstract sealed class DomainException extends RuntimeException
    permits NotFoundException, ConflictException, RuleViolationException {

  private final transient Object[] arguments;

  protected DomainException(String message, Object... arguments) {
    super(message);
    this.arguments = arguments.clone();
  }

  /** Values that the localized message shows, in order. */
  public Object[] arguments() {
    return arguments.clone();
  }

  /**
   * Extra members of the problem detail, such as the items that broke the rule, so a client can
   * show them. Empty unless an exception overrides it.
   */
  public Map<String, Object> properties() {
    return Map.of();
  }
}
