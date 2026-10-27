package com.waqtalabs.auxia.identityaccess.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifier of a user account. Other contexts keep it as a typed reference, such as the
 * AuthorityId of Emergency Management.
 */
public record UserId(UUID value) {

  public UserId {
    Objects.requireNonNull(value, "value");
  }

  public static UserId generate() {
    return new UserId(UUID.randomUUID());
  }
}
