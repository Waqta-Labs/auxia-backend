package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifier of the SOS of a citizen. The phone generates it (clientReportId), so the same SOS
 * sent twice has the same identifier.
 */
public record CitizenReportId(UUID value) {

  public CitizenReportId {
    Objects.requireNonNull(value, "value");
  }
}
