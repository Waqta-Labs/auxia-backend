package com.waqtalabs.auxia.shared.application.exceptions;

/**
 * An external service, such as the AI Service, did not answer or answered with an error. The
 * adapters of the outgoing ports throw it, and the REST layer answers 503.
 */
public class ExternalServiceUnavailableException extends RuntimeException {

  private final String serviceName;

  public ExternalServiceUnavailableException(String serviceName, Throwable cause) {
    super(serviceName + " is not available", cause);
    this.serviceName = serviceName;
  }

  /** Name of the service, shown in the error message. */
  public String serviceName() {
    return serviceName;
  }
}
