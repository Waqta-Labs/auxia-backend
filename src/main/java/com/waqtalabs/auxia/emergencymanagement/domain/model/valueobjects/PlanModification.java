package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.time.LocalDateTime;
import java.util.Objects;

/** Who changed the quantities of a plan before deciding it, when and why (US-13, US-14). */
public record PlanModification(AuthorityId modifiedBy, LocalDateTime modifiedAt, String reason) {

  public PlanModification {
    Objects.requireNonNull(modifiedBy, "modifiedBy");
    Objects.requireNonNull(modifiedAt, "modifiedAt");
    Objects.requireNonNull(reason, "reason");
  }
}
