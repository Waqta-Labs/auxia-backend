package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Account of Identity Access that a member of a brigade uses in the mobile app. */
public record UserId(UUID value) {

  public UserId {
    Objects.requireNonNull(value, "value");
  }
}
