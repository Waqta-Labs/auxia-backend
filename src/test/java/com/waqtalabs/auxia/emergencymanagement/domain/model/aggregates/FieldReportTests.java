package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportAlreadyAssignedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneOutsideEmergencyException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class FieldReportTests {

  private static final LocalDateTime REPORTED_AT = LocalDateTime.of(2026, 3, 14, 9, 30);
  private static final String TEXT = "Derrumbe de viviendas en el sector Alto Perú.";

  private final Emergency emergency = emergency("Huaico Chosica 2026");
  private final Zone zone =
      emergency.registerZone(
          "Sector Alto Perú", null, new PopulationImpact(null, 0, 0, 0, 0, 0), DamageLevel.SEVERE);

  @Test
  void registeredReportIsPendingZoneAssignment() {
    FieldReport report =
        FieldReport.register(emergency.id(), " " + TEXT + " ", ReportSource.VOLUNTEER, REPORTED_AT);

    assertThat(report.id()).isNotNull();
    assertThat(report.emergencyId()).isEqualTo(emergency.id());
    assertThat(report.rawText()).isEqualTo(TEXT);
    assertThat(report.source()).isEqualTo(ReportSource.VOLUNTEER);
    assertThat(report.reportedAt()).isEqualTo(REPORTED_AT);
    assertThat(report.zoneId()).isEmpty();
    assertThat(report.isPendingZoneAssignment()).isTrue();
  }

  @Test
  void assigningAZoneOfTheSameEmergencyAssociatesTheReport() {
    FieldReport report = register();

    report.assignToZone(zone);

    assertThat(report.zoneId()).contains(zone.id());
    assertThat(report.isPendingZoneAssignment()).isFalse();
  }

  @Test
  void reportIsAssignedOnlyOnce() {
    FieldReport report = register();
    report.assignToZone(zone);

    assertThatThrownBy(() -> report.assignToZone(zone))
        .isInstanceOf(FieldReportAlreadyAssignedException.class);
  }

  @Test
  void zoneOfAnotherEmergencyIsRejected() {
    FieldReport report = register();
    Zone otherZone =
        emergency("Inundación Piura 2026")
            .registerZone(
                "Castilla", null, new PopulationImpact(null, 0, 0, 0, 0, 0), DamageLevel.LOW);

    assertThatThrownBy(() -> report.assignToZone(otherZone))
        .isInstanceOf(ZoneOutsideEmergencyException.class);
    assertThat(report.isPendingZoneAssignment()).isTrue();
  }

  @Test
  void blankTextIsRejected() {
    assertThatThrownBy(
            () -> FieldReport.register(emergency.id(), "  ", ReportSource.CITIZEN, REPORTED_AT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reconstitutedReportKeepsItsZone() {
    FieldReport stored = register();

    FieldReport report =
        FieldReport.reconstitute(
            stored.id(),
            stored.emergencyId(),
            zone.id(),
            stored.rawText(),
            stored.source(),
            stored.reportedAt(),
            null);

    assertThat(report.id()).isEqualTo(stored.id());
    assertThat(report.zoneId()).contains(zone.id());
  }

  private FieldReport register() {
    return FieldReport.register(emergency.id(), TEXT, ReportSource.AUTHORITY, REPORTED_AT);
  }

  private static Emergency emergency(String name) {
    return Emergency.register(
        name,
        EmergencyType.LANDSLIDE,
        null,
        new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica"),
        LocalDate.of(2026, 3, 14));
  }
}
