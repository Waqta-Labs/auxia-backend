package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Organization of Identity Access, referenced by its identifier. */
public record OrganizationId(UUID value) {

  public OrganizationId {
    Objects.requireNonNull(value, "value");
  }
}
