package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Organization of Identity Access whose inventory a distribution plan uses. */
public record OrganizationId(UUID value) {

  public OrganizationId {
    Objects.requireNonNull(value, "value");
  }
}
