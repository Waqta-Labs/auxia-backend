package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/** SHA-256 hash, as 64 lowercase hexadecimal characters. */
public record HashValue(String value) {

  private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

  public HashValue {
    Objects.requireNonNull(value, "value");
    if (!SHA_256.matcher(value).matches()) {
      throw new IllegalArgumentException("value must be 64 lowercase hexadecimal characters");
    }
  }

  /** The hash of a 32-byte digest. */
  public static HashValue of(byte[] digest) {
    return new HashValue(HexFormat.of().formatHex(digest));
  }

  /** The 32 bytes of the hash, as the smart contract stores them. */
  public byte[] bytes() {
    return HexFormat.of().parseHex(value);
  }
}
