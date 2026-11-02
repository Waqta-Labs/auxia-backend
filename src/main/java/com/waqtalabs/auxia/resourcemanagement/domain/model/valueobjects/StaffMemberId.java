package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a member of a brigade. */
public record StaffMemberId(UUID value) {

  public StaffMemberId {
    Objects.requireNonNull(value, "value");
  }

  public static StaffMemberId generate() {
    return new StaffMemberId(UUID.randomUUID());
  }
}
