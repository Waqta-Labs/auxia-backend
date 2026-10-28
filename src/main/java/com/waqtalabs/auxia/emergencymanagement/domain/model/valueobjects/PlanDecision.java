package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.time.LocalDateTime;
import java.util.Objects;

/** The authority who approved or rejected a plan, and when (TS-C01). */
public record PlanDecision(AuthorityId decidedBy, LocalDateTime decidedAt) {

  public PlanDecision {
    Objects.requireNonNull(decidedBy, "decidedBy");
    Objects.requireNonNull(decidedAt, "decidedAt");
  }
}
