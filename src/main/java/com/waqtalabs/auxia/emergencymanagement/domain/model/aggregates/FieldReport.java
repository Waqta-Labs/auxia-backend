package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportAlreadyAssignedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportAlreadyProcessedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneOutsideEmergencyException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Field report written in natural language (US-01).
 *
 * <p>A report always belongs to an emergency. Its zone is assigned when it is known; until then
 * the report is pending zone assignment (US-01, scenario 2). The NLP structures its text once,
 * and from then on the report is processed (US-02).
 */
public final class FieldReport {

  private final FieldReportId id;
  private final EmergencyId emergencyId;
  private final String rawText;
  private final ReportSource source;
  private final LocalDateTime reportedAt;
  private ZoneId zoneId;
  private StructuredFieldData structuredData;

  private FieldReport(
      FieldReportId id,
      EmergencyId emergencyId,
      ZoneId zoneId,
      String rawText,
      ReportSource source,
      LocalDateTime reportedAt,
      StructuredFieldData structuredData) {
    this.id = Objects.requireNonNull(id, "id");
    this.emergencyId = Objects.requireNonNull(emergencyId, "emergencyId");
    this.zoneId = zoneId;
    this.rawText = requireText(rawText);
    this.source = Objects.requireNonNull(source, "source");
    this.reportedAt = Objects.requireNonNull(reportedAt, "reportedAt");
    this.structuredData = structuredData;
  }

  /** Registers a report of the emergency, still without a zone and not processed. */
  public static FieldReport register(
      EmergencyId emergencyId, String rawText, ReportSource source, LocalDateTime reportedAt) {
    return new FieldReport(
        FieldReportId.generate(), emergencyId, null, rawText, source, reportedAt, null);
  }

  /**
   * Rebuilds a report that is already stored, without registering it again.
   *
   * @param structuredData {@code null} while the report is not processed
   */
  public static FieldReport reconstitute(
      FieldReportId id,
      EmergencyId emergencyId,
      ZoneId zoneId,
      String rawText,
      ReportSource source,
      LocalDateTime reportedAt,
      StructuredFieldData structuredData) {
    return new FieldReport(id, emergencyId, zoneId, rawText, source, reportedAt, structuredData);
  }

  /**
   * Stores the indicators that the NLP extracted from the text.
   *
   * @throws FieldReportAlreadyProcessedException if the report was already structured
   */
  public void markProcessed(StructuredFieldData structuredData) {
    Objects.requireNonNull(structuredData, "structuredData");
    if (isProcessed()) {
      throw new FieldReportAlreadyProcessedException(id);
    }
    this.structuredData = structuredData;
  }

  public boolean isProcessed() {
    return structuredData != null;
  }

  /**
   * Associates the report with a zone of its emergency. A report is assigned only once.
   *
   * @throws FieldReportAlreadyAssignedException if the report already has a zone
   * @throws ZoneOutsideEmergencyException if the zone belongs to another emergency
   */
  public void assignToZone(Zone zone) {
    Objects.requireNonNull(zone, "zone");
    if (!isPendingZoneAssignment()) {
      throw new FieldReportAlreadyAssignedException(id);
    }
    if (!zone.belongsTo(emergencyId)) {
      throw new ZoneOutsideEmergencyException(zone.id(), emergencyId);
    }
    zoneId = zone.id();
  }

  public boolean isPendingZoneAssignment() {
    return zoneId == null;
  }

  public FieldReportId id() {
    return id;
  }

  public EmergencyId emergencyId() {
    return emergencyId;
  }

  public Optional<ZoneId> zoneId() {
    return Optional.ofNullable(zoneId);
  }

  public String rawText() {
    return rawText;
  }

  public ReportSource source() {
    return source;
  }

  public LocalDateTime reportedAt() {
    return reportedAt;
  }

  /** Empty while the report is not processed. */
  public Optional<StructuredFieldData> structuredData() {
    return Optional.ofNullable(structuredData);
  }

  private static String requireText(String value) {
    Objects.requireNonNull(value, "rawText");
    if (value.isBlank()) {
      throw new IllegalArgumentException("rawText must not be blank");
    }
    return value.strip();
  }
}
