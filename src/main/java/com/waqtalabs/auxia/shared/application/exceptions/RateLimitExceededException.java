package com.waqtalabs.auxia.shared.application.exceptions;

import java.time.Duration;
import java.util.Objects;

/**
 * A sender made more requests than allowed in a period. The REST layer answers 429 with a
 * Retry-After header, so the client knows when to try again.
 */
public class RateLimitExceededException extends RuntimeException {

  private final Duration retryAfter;

  public RateLimitExceededException(Duration retryAfter) {
    super("Too many requests; retry after " + retryAfter.toSeconds() + " s");
    this.retryAfter = Objects.requireNonNull(retryAfter, "retryAfter");
  }

  /** How long the sender must wait before the next request is accepted. */
  public Duration retryAfter() {
    return retryAfter;
  }
}
