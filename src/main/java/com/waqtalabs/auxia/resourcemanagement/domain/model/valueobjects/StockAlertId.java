package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a low stock alert. */
public record StockAlertId(UUID value) {

  public StockAlertId {
    Objects.requireNonNull(value, "value");
  }

  public static StockAlertId generate() {
    return new StockAlertId(UUID.randomUUID());
  }
}
