package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Indicators of a field report corrected by hand by an authority (US-03). The report keeps the
 * indicators of the NLP next to them, so the original and the corrected values can be told apart.
 */
public record ReportCorrection(
    StructuredFieldData correctedData,
    AuthorityId correctedBy,
    LocalDateTime correctedAt,
    String reason) {

  public ReportCorrection {
    Objects.requireNonNull(correctedData, "correctedData");
    Objects.requireNonNull(correctedBy, "correctedBy");
    Objects.requireNonNull(correctedAt, "correctedAt");
    Objects.requireNonNull(reason, "reason");
  }
}
