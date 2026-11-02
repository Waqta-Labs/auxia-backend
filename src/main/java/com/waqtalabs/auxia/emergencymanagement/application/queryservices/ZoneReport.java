package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything the exported report of a zone shows: its emergency, its impact, its urgency score
 * with the explanation and its field reports (US-05, scenario 2).
 *
 * @param fieldReports from the most recent
 */
public record ZoneReport(
    Emergency emergency, Zone zone, List<FieldReport> fieldReports, LocalDateTime generatedAt) {

  public ZoneReport {
    fieldReports = List.copyOf(fieldReports);
  }
}
