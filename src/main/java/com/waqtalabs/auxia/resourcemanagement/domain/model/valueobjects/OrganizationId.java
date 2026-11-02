package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Reference to an organization of Identity Access. */
public record OrganizationId(UUID value) {

  public OrganizationId {
    Objects.requireNonNull(value, "value");
  }
}
