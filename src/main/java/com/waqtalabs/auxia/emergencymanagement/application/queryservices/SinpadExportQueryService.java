package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.application.ports.SinpadExportWriter;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetSinpadExportQuery;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.IncompleteSinpadReportsException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.IncompleteSinpadReportsException.IncompleteReport;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.NoFieldReportsToExportException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exports the field reports of an emergency in the layout of SINPAD, so the authority does not
 * register them twice (US-33). SINPAD requires the zone, the damage level, the people and the
 * families affected of each report; if one report lacks any, nothing is exported and every
 * incomplete report is listed with its missing fields (scenario 2). Corrected indicators are used
 * when a report was corrected (US-03).
 */
@Service
@Transactional(readOnly = true)
public class SinpadExportQueryService {

  private final EmergencyRepository emergencyRepository;
  private final ZoneRepository zoneRepository;
  private final FieldReportRepository fieldReportRepository;
  private final SinpadExportWriter writer;

  public SinpadExportQueryService(
      EmergencyRepository emergencyRepository,
      ZoneRepository zoneRepository,
      FieldReportRepository fieldReportRepository,
      SinpadExportWriter writer) {
    this.emergencyRepository = emergencyRepository;
    this.zoneRepository = zoneRepository;
    this.fieldReportRepository = fieldReportRepository;
    this.writer = writer;
  }

  /**
   * @throws EmergencyNotFoundException if the emergency does not exist
   * @throws NoFieldReportsToExportException if the emergency has no field reports
   * @throws IncompleteSinpadReportsException if a report lacks a field SINPAD requires
   */
  public byte[] handle(GetSinpadExportQuery query) {
    Emergency emergency =
        emergencyRepository
            .findById(query.emergencyId())
            .orElseThrow(() -> new EmergencyNotFoundException(query.emergencyId()));
    List<FieldReport> reports =
        fieldReportRepository.findByEmergencyId(emergency.id()).stream()
            .sorted(Comparator.comparing(FieldReport::reportedAt))
            .toList();
    if (reports.isEmpty()) {
      throw new NoFieldReportsToExportException(emergency.id());
    }
    List<IncompleteReport> incomplete =
        reports.stream()
            .map(report -> new IncompleteReport(report.id(), missingFields(report)))
            .filter(report -> !report.missingFields().isEmpty())
            .toList();
    if (!incomplete.isEmpty()) {
      throw new IncompleteSinpadReportsException(incomplete);
    }
    Map<ZoneId, String> zoneNames =
        zoneRepository.findByEmergencyId(emergency.id()).stream()
            .collect(Collectors.toMap(Zone::id, Zone::name));
    List<SinpadExport.Row> rows =
        reports.stream()
            .map(
                report ->
                    new SinpadExport.Row(
                        report,
                        zoneNames.get(report.zoneId().orElseThrow()),
                        report.structuredData().orElseThrow()))
            .toList();
    return writer.write(new SinpadExport(emergency, rows));
  }

  /** A report not structured yet lacks every indicator. */
  private static List<String> missingFields(FieldReport report) {
    List<String> missing = new ArrayList<>();
    if (report.zoneId().isEmpty()) {
      missing.add("zone");
    }
    StructuredFieldData data = report.structuredData().orElse(null);
    if (data == null || data.damageLevel() == null) {
      missing.add("damageLevel");
    }
    if (data == null || data.peopleAffected() == null) {
      missing.add("peopleAffected");
    }
    if (data == null || data.familiesAffected() == null) {
      missing.add("familiesAffected");
    }
    return missing;
  }
}
