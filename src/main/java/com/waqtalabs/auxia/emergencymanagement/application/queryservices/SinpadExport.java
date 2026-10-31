package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import java.util.List;

/**
 * Field reports of an emergency ready for SINPAD (US-33), from the oldest. Every row has the fields
 * SINPAD requires.
 */
public record SinpadExport(Emergency emergency, List<Row> rows) {

  /** A field report with the name of its zone and the indicators in use, corrected or not. */
  public record Row(FieldReport report, String zoneName, StructuredFieldData data) {}
}
