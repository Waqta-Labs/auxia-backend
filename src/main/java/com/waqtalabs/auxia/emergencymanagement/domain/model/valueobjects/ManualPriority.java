package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * An authority marked a zone as a priority by hand, for a risk the score does not capture, such as
 * an imminent one (US-07).
 */
public record ManualPriority(AuthorityId setBy, LocalDateTime setAt, String reason) {

  public ManualPriority {
    Objects.requireNonNull(setBy, "setBy");
    Objects.requireNonNull(setAt, "setAt");
    Objects.requireNonNull(reason, "reason");
  }
}
