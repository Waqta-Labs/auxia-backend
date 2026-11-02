package com.waqtalabs.auxia.identityaccess.domain.model.valueobjects;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Email with which a user signs in. It is kept in lowercase, so two spellings of the same address
 * are the same user.
 */
public record EmailAddress(String value) {

  private static final Pattern FORMAT = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
  private static final int MAX_LENGTH = 254;

  public EmailAddress {
    Objects.requireNonNull(value, "value");
    value = value.strip().toLowerCase(Locale.ROOT);
    if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
      throw new IllegalArgumentException("invalid email address");
    }
  }
}
