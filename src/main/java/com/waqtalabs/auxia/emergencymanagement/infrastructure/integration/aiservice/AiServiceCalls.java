package com.waqtalabs.auxia.emergencymanagement.infrastructure.integration.aiservice;

import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;

/** Turns any failure of a call to the AI Service into ExternalServiceUnavailableException. */
final class AiServiceCalls {

  static final String SERVICE_NAME = "AI Service";

  private static final Logger LOGGER = LoggerFactory.getLogger(AiServiceCalls.class);

  private AiServiceCalls() {}

  /**
   * Runs a call and returns its response body. A connection error, a timeout, an error status, a
   * body that does not match the contract or an empty body make the service unavailable.
   */
  static <T> T call(String endpoint, Supplier<T> request) {
    T body;
    try {
      body = request.get();
    } catch (RestClientException ex) {
      LOGGER.warn("Call to {} {} failed: {}", SERVICE_NAME, endpoint, ex.getMessage());
      throw new ExternalServiceUnavailableException(SERVICE_NAME, ex);
    }
    if (body == null) {
      LOGGER.warn("Call to {} {} returned an empty body", SERVICE_NAME, endpoint);
      throw new ExternalServiceUnavailableException(SERVICE_NAME, null);
    }
    return body;
  }
}
