package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of the assignment of a brigade to a distribution plan. */
public record AssignmentId(UUID value) {

  public AssignmentId {
    Objects.requireNonNull(value, "value");
  }

  public static AssignmentId generate() {
    return new AssignmentId(UUID.randomUUID());
  }
}
