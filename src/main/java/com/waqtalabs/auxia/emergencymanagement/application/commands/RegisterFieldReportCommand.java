package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;

/**
 * Registers a field report (US-01).
 *
 * @param emergencyId emergency of the report, or {@code null} to take it from the zone
 * @param zoneId zone indicated by the authority, or {@code null} to identify it from the text
 */
public record RegisterFieldReportCommand(
    EmergencyId emergencyId, ZoneId zoneId, String rawText, ReportSource source) {

  public RegisterFieldReportCommand {
    if (emergencyId == null && zoneId == null) {
      throw new IllegalArgumentException("A field report needs an emergency or a zone");
    }
  }

  /** The zone comes in the request: the report belongs to the emergency of that zone. */
  public static RegisterFieldReportCommand forZone(
      ZoneId zoneId, String rawText, ReportSource source) {
    return new RegisterFieldReportCommand(null, zoneId, rawText, source);
  }
}
