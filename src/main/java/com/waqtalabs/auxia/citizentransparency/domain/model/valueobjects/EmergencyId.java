package com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Emergency of Emergency Management: the same identifier, copied by the projection. */
public record EmergencyId(UUID value) {

  public EmergencyId {
    Objects.requireNonNull(value, "value");
  }
}
