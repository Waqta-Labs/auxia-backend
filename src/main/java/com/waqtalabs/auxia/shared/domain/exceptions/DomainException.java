package com.waqtalabs.auxia.shared.domain.exceptions;

/**
 * Base of the errors raised by the domain of any bounded context.
 *
 * <p>The message is the English description for logs. The REST layer looks up the localized
 * detail by the exception class and fills it with {@link #arguments()}.
 */
public abstract class DomainException extends RuntimeException {

  private final transient Object[] arguments;

  protected DomainException(String message, Object... arguments) {
    super(message);
    this.arguments = arguments.clone();
  }

  /** Values that the localized message shows, in order. */
  public Object[] arguments() {
    return arguments.clone();
  }
}
