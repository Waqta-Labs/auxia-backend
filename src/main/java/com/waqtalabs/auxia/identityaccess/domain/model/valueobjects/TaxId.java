package com.waqtalabs.auxia.identityaccess.domain.model.valueobjects;

import java.util.Objects;
import java.util.regex.Pattern;

/** Peruvian taxpayer number (RUC) of an organization: 11 digits. */
public record TaxId(String value) {

  private static final Pattern RUC = Pattern.compile("\\d{11}");

  public TaxId {
    Objects.requireNonNull(value, "value");
    value = value.strip();
    if (!RUC.matcher(value).matches()) {
      throw new IllegalArgumentException("a RUC has 11 digits");
    }
  }
}
