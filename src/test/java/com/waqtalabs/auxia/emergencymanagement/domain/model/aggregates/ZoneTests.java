package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyClosedException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ZoneTests {

  private static final Geolocation ALTO_PERU = new Geolocation(-11.9436, -76.7078);
  private static final PopulationImpact IMPACT = new PopulationImpact(600, 35, 140, 40, 15, 0);

  private final Emergency emergency =
      Emergency.register(
          "Huaico Chosica 2026",
          EmergencyType.LANDSLIDE,
          null,
          new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica"),
          LocalDate.of(2026, 3, 14));

  @Test
  void activeEmergencyRegistersZonesInRegisteredStatus() {
    Zone zone =
        emergency.registerZone(" Sector Alto Perú ", ALTO_PERU, IMPACT, DamageLevel.SEVERE);

    assertThat(zone.id()).isNotNull();
    assertThat(zone.emergencyId()).isEqualTo(emergency.id());
    assertThat(zone.belongsTo(emergency.id())).isTrue();
    assertThat(zone.name()).isEqualTo("Sector Alto Perú");
    assertThat(zone.geolocation()).contains(ALTO_PERU);
    assertThat(zone.populationImpact()).isEqualTo(IMPACT);
    assertThat(zone.damageLevel()).isEqualTo(DamageLevel.SEVERE);
    assertThat(zone.status()).isEqualTo(ZoneStatus.REGISTERED);
  }

  @Test
  void geolocationIsOptional() {
    Zone zone = emergency.registerZone("Quebrada Pedregal", null, IMPACT, DamageLevel.LOW);

    assertThat(zone.geolocation()).isEmpty();
  }

  @Test
  void closedEmergencyRejectsNewZones() {
    emergency.close();

    assertThatThrownBy(
            () -> emergency.registerZone("Sector Alto Perú", null, IMPACT, DamageLevel.SEVERE))
        .isInstanceOf(EmergencyClosedException.class);
  }

  @Test
  void blankNameIsRejected() {
    assertThatThrownBy(() -> emergency.registerZone(" ", null, IMPACT, DamageLevel.SEVERE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void zoneDoesNotBelongToAnotherEmergency() {
    Zone zone = emergency.registerZone("Sector Alto Perú", null, IMPACT, DamageLevel.SEVERE);

    assertThat(zone.belongsTo(EmergencyId.generate())).isFalse();
  }

  @Test
  void reconstitutedZoneKeepsItsStatus() {
    Zone stored = emergency.registerZone("Sector Alto Perú", null, IMPACT, DamageLevel.SEVERE);

    Zone zone =
        Zone.reconstitute(
            stored.id(),
            stored.emergencyId(),
            stored.name(),
            ALTO_PERU,
            stored.populationImpact(),
            stored.damageLevel(),
            ZoneStatus.PRIORITIZED);

    assertThat(zone.id()).isEqualTo(stored.id());
    assertThat(zone.status()).isEqualTo(ZoneStatus.PRIORITIZED);
    assertThat(zone.geolocation()).contains(ALTO_PERU);
  }
}
