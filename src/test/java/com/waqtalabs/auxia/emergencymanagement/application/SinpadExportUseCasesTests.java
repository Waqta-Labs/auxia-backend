package com.waqtalabs.auxia.emergencymanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.waqtalabs.auxia.emergencymanagement.application.queries.GetSinpadExportQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.SinpadExport;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.SinpadExportQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.IncompleteSinpadReportsException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.IncompleteSinpadReportsException.IncompleteReport;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.NoFieldReportsToExportException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Export of the field reports of an emergency for SINPAD (US-33). */
class SinpadExportUseCasesTests {

  private final InMemoryEmergencyRepository emergencies = new InMemoryEmergencyRepository();
  private final InMemoryZoneRepository zones = new InMemoryZoneRepository();
  private final InMemoryFieldReportRepository reports = new InMemoryFieldReportRepository();
  private final List<SinpadExport> written = new ArrayList<>();
  private final SinpadExportQueryService exports =
      new SinpadExportQueryService(
          emergencies,
          zones,
          reports,
          export -> {
            written.add(export);
            return new byte[] {1};
          });

  private final Emergency emergency =
      emergencies.save(
          Emergency.register(
              "Huaico Chosica 2026",
              EmergencyType.LANDSLIDE,
              null,
              new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica"),
              LocalDate.of(2026, 3, 14)));
  private final Zone zone =
      zones.save(
          emergency.registerZone(
              "Sector Alto Perú",
              null,
              new PopulationImpact(600, 35, 140, 40, 15, 0),
              DamageLevel.SEVERE));

  @Test
  void everyReportIsExportedFromTheOldestWithItsCorrectedIndicators() {
    FieldReport later = report(zone, hour(12), complete(20, 5));
    FieldReport corrected = report(zone, hour(9), complete(140, 35));
    corrected.correct(
        complete(400, 90),
        new AuthorityId(UUID.randomUUID()),
        hour(10),
        "Conteo en campo de la brigada");
    reports.save(corrected);

    byte[] csv = exports.handle(new GetSinpadExportQuery(emergency.id()));

    assertThat(csv).containsExactly(1);
    SinpadExport export = written.getFirst();
    assertThat(export.emergency()).isEqualTo(emergency);
    assertThat(export.rows())
        .extracting(row -> row.report().id())
        .containsExactly(corrected.id(), later.id());
    assertThat(export.rows().getFirst().zoneName()).isEqualTo("Sector Alto Perú");
    assertThat(export.rows().getFirst().data().peopleAffected()).isEqualTo(400);
  }

  @Test
  void aReportWithoutARequiredFieldStopsTheExportAndIsListed() {
    report(zone, hour(8), complete(140, 35));
    FieldReport withoutFamilies =
        report(
            zone,
            hour(9),
            new StructuredFieldData(
                DamageLevel.SEVERE,
                null,
                140,
                null,
                null,
                null,
                Set.of(Need.WATER),
                List.of("familiesAffected")));
    FieldReport pending =
        reports.save(
            FieldReport.register(
                emergency.id(), "Sin datos todavía", ReportSource.CITIZEN, hour(10)));

    var error =
        catchThrowableOfType(
            IncompleteSinpadReportsException.class,
            () -> exports.handle(new GetSinpadExportQuery(emergency.id())));

    assertThat(error.reports())
        .containsExactly(
            new IncompleteReport(withoutFamilies.id(), List.of("familiesAffected")),
            new IncompleteReport(
                pending.id(),
                List.of("zone", "damageLevel", "peopleAffected", "familiesAffected")));
    assertThat(error.properties())
        .isEqualTo(
            Map.of(
                "incompleteReports",
                List.of(
                    Map.of(
                        "fieldReportId",
                        withoutFamilies.id().value(),
                        "missingFields",
                        List.of("familiesAffected")),
                    Map.of(
                        "fieldReportId",
                        pending.id().value(),
                        "missingFields",
                        List.of("zone", "damageLevel", "peopleAffected", "familiesAffected")))));
    assertThat(written).isEmpty();
  }

  @Test
  void anEmergencyWithoutReportsOrUnknownHasNothingToExport() {
    assertThat(
            catchThrowableOfType(
                NoFieldReportsToExportException.class,
                () -> exports.handle(new GetSinpadExportQuery(emergency.id()))))
        .isNotNull();
    assertThat(
            catchThrowableOfType(
                EmergencyNotFoundException.class,
                () -> exports.handle(new GetSinpadExportQuery(EmergencyId.generate()))))
        .isNotNull();
  }

  private FieldReport report(Zone zone, LocalDateTime at, StructuredFieldData data) {
    FieldReport report =
        FieldReport.register(
            emergency.id(), "Reporte de " + zone.name(), ReportSource.AUTHORITY, at);
    report.assignToZone(zone);
    report.markProcessed(data);
    return reports.save(report);
  }

  private static StructuredFieldData complete(int people, int families) {
    return new StructuredFieldData(
        DamageLevel.SEVERE, families, people, null, null, null, Set.of(Need.WATER), List.of());
  }

  private static LocalDateTime hour(int hour) {
    return LocalDateTime.of(2026, 3, 15, hour, 0);
  }
}
