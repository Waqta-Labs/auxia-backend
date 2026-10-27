package com.waqtalabs.auxia.identityaccess.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of an organization. Other contexts keep it as a typed reference. */
public record OrganizationId(UUID value) {

  public OrganizationId {
    Objects.requireNonNull(value, "value");
  }

  public static OrganizationId generate() {
    return new OrganizationId(UUID.randomUUID());
  }
}
