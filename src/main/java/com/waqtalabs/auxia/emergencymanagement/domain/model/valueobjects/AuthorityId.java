package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** User of Identity Access with the AUTHORITY role who modifies, approves or rejects a plan. */
public record AuthorityId(UUID value) {

  public AuthorityId {
    Objects.requireNonNull(value, "value");
  }
}
