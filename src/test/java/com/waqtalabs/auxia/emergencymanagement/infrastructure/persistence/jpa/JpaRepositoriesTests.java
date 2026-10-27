package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyStatus;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** The JPA adapters store and rebuild the aggregates without losing any value. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class JpaRepositoriesTests {

  private static final AdministrativeLocation CHOSICA =
      new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica");
  private static final LocalDateTime REPORTED_AT = LocalDateTime.of(2026, 3, 14, 9, 30);

  @Autowired private EmergencyRepository emergencies;
  @Autowired private ZoneRepository zones;
  @Autowired private FieldReportRepository fieldReports;

  @Test
  void emergencyIsStoredAndRebuilt() {
    Emergency emergency = emergencies.save(registerEmergency("Huaico Chosica 2026"));

    Emergency found = emergencies.findById(emergency.id()).orElseThrow();

    assertThat(found.name()).isEqualTo("Huaico Chosica 2026");
    assertThat(found.type()).isEqualTo(EmergencyType.LANDSLIDE);
    assertThat(found.description()).contains("Lluvias intensas");
    assertThat(found.location()).isEqualTo(CHOSICA);
    assertThat(found.startDate()).isEqualTo(LocalDate.of(2026, 3, 14));
    assertThat(found.status()).isEqualTo(EmergencyStatus.ACTIVE);
  }

  @Test
  void closingAnEmergencyUpdatesItAndRemovesItFromTheActiveList() {
    Emergency active = emergencies.save(registerEmergency("Inundación Piura 2026"));
    Emergency closed = emergencies.save(registerEmergency("Incendio Cusco 2026"));
    closed.close();
    emergencies.save(closed);

    assertThat(emergencies.findById(closed.id()).orElseThrow().isActive()).isFalse();
    assertThat(emergencies.findActive())
        .extracting(Emergency::id)
        .contains(active.id())
        .doesNotContain(closed.id());
  }

  @Test
  void unknownEmergencyIsNotFound() {
    assertThat(emergencies.findById(new EmergencyId(UUID.randomUUID()))).isEmpty();
  }

  @Test
  void zoneIsStoredAndRebuiltWithItsGeolocationAndPopulation() {
    Emergency emergency = emergencies.save(registerEmergency("Huaico Chosica 2026"));
    var impact = new PopulationImpact(600, 35, 140, 40, 15, 6);
    Zone zone =
        zones.save(
            emergency.registerZone(
                "Sector Alto Perú",
                new Geolocation(-11.9436, -76.7078),
                impact,
                DamageLevel.SEVERE));

    Zone found = zones.findById(zone.id()).orElseThrow();

    assertThat(found.emergencyId()).isEqualTo(emergency.id());
    assertThat(found.name()).isEqualTo("Sector Alto Perú");
    assertThat(found.geolocation()).contains(new Geolocation(-11.9436, -76.7078));
    assertThat(found.populationImpact()).isEqualTo(impact);
    assertThat(found.damageLevel()).isEqualTo(DamageLevel.SEVERE);
    assertThat(found.status()).isEqualTo(zone.status());
  }

  @Test
  void zoneWithoutGeolocationOrPopulationKeepsThemEmpty() {
    Emergency emergency = emergencies.save(registerEmergency("Huaico Chosica 2026"));
    Zone zone = zones.save(registerZone(emergency, "Quebrada Pedregal"));

    Zone found = zones.findById(zone.id()).orElseThrow();

    assertThat(found.geolocation()).isEmpty();
    assertThat(found.populationImpact().knownPopulation()).isEmpty();
  }

  @Test
  void zonesOfAnEmergencyKeepTheirRegistrationOrder() {
    Emergency emergency = emergencies.save(registerEmergency("Huaico Chosica 2026"));
    Emergency other = emergencies.save(registerEmergency("Inundación Piura 2026"));
    Zone first = zones.save(registerZone(emergency, "Sector Alto Perú"));
    Zone second = zones.save(registerZone(emergency, "Quebrada Pedregal"));
    zones.save(registerZone(other, "Castilla"));

    assertThat(zones.findByEmergencyId(emergency.id()))
        .extracting(Zone::id)
        .containsExactly(first.id(), second.id());
  }

  @Test
  void unknownZoneIsNotFound() {
    assertThat(zones.findById(new ZoneId(UUID.randomUUID()))).isEmpty();
  }

  @Test
  void pendingFieldReportIsStoredAndLaterAssigned() {
    Emergency emergency = emergencies.save(registerEmergency("Huaico Chosica 2026"));
    Zone zone = zones.save(registerZone(emergency, "Sector Alto Perú"));
    FieldReport report =
        fieldReports.save(
            FieldReport.register(
                emergency.id(), "Derrumbe de viviendas", ReportSource.VOLUNTEER, REPORTED_AT));

    assertThat(fieldReports.findPendingZoneAssignmentByEmergencyId(emergency.id()))
        .extracting(FieldReport::id)
        .containsExactly(report.id());

    report.assignToZone(zone);
    fieldReports.save(report);

    FieldReport found = fieldReports.findById(report.id()).orElseThrow();
    assertThat(found.zoneId()).contains(zone.id());
    assertThat(found.rawText()).isEqualTo("Derrumbe de viviendas");
    assertThat(found.source()).isEqualTo(ReportSource.VOLUNTEER);
    assertThat(found.reportedAt()).isEqualTo(REPORTED_AT);
    assertThat(fieldReports.findPendingZoneAssignmentByEmergencyId(emergency.id())).isEmpty();
    assertThat(fieldReports.findByZoneId(zone.id()))
        .extracting(FieldReport::id)
        .containsExactly(report.id());
  }

  @Test
  void fieldReportsOfAnEmergencyGoFromTheMostRecent() {
    Emergency emergency = emergencies.save(registerEmergency("Huaico Chosica 2026"));
    FieldReport older =
        fieldReports.save(
            FieldReport.register(
                emergency.id(), "Primer reporte", ReportSource.CITIZEN, REPORTED_AT));
    FieldReport newer =
        fieldReports.save(
            FieldReport.register(
                emergency.id(),
                "Segundo reporte",
                ReportSource.CITIZEN,
                REPORTED_AT.plusHours(2)));

    assertThat(fieldReports.findByEmergencyId(emergency.id()))
        .extracting(FieldReport::id)
        .containsExactly(newer.id(), older.id());
  }

  @Test
  void unknownFieldReportIsNotFound() {
    assertThat(fieldReports.findById(new FieldReportId(UUID.randomUUID()))).isEmpty();
  }

  private static Emergency registerEmergency(String name) {
    return Emergency.register(
        name, EmergencyType.LANDSLIDE, "Lluvias intensas", CHOSICA, LocalDate.of(2026, 3, 14));
  }

  private static Zone registerZone(Emergency emergency, String name) {
    return emergency.registerZone(
        name, null, new PopulationImpact(null, 0, 0, 0, 0, 0), DamageLevel.MODERATE);
  }
}
