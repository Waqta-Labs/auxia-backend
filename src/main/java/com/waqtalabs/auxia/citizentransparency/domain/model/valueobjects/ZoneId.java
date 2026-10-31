package com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Zone of Emergency Management: the same identifier, copied by the projection. */
public record ZoneId(UUID value) {

  public ZoneId {
    Objects.requireNonNull(value, "value");
  }
}
