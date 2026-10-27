package com.waqtalabs.auxia.identityaccess.domain.model.valueobjects;

import java.util.Objects;

/**
 * Hash of a password and the algorithm that produced it. The password itself is never kept.
 *
 * @param algorithm such as {@code BCRYPT}
 */
public record PasswordHash(String value, String algorithm) {

  public PasswordHash {
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(algorithm, "algorithm");
    if (value.isBlank() || algorithm.isBlank()) {
      throw new IllegalArgumentException("hash and algorithm must not be blank");
    }
  }

  /** Never shows the hash, so it does not end up in a log. */
  @Override
  public String toString() {
    return "PasswordHash[" + algorithm + "]";
  }
}
