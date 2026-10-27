package com.waqtalabs.auxia.emergencymanagement.domain.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ZoneIdentificationServiceTests {

  private final ZoneIdentificationService service = new ZoneIdentificationService();

  private final Emergency emergency =
      Emergency.register(
          "Huaico Chosica 2026",
          EmergencyType.LANDSLIDE,
          null,
          new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica"),
          LocalDate.of(2026, 3, 14));
  private final Zone altoPeru = zone("Alto Perú");
  private final Zone pedregal = zone("Quebrada Pedregal");
  private final List<Zone> zones = List.of(altoPeru, pedregal);

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Derrumbe de viviendas en el sector Alto Perú.",
        "derrumbe en ALTO PERU, urge agua",
        "Familias de alto   perú sin techo"
      })
  void identifiesTheZoneMentionedIgnoringCaseAccentsAndSpaces(String text) {
    assertThat(service.identifyZone(text, zones)).contains(altoPeru);
  }

  @Test
  void reportWithoutAnyZoneNameIsNotIdentified() {
    assertThat(service.identifyZone("Hay 35 familias afectadas.", zones)).isEmpty();
  }

  @Test
  void reportMentioningSeveralZonesIsNotIdentified() {
    String text = "El huaico afectó Alto Perú y la Quebrada Pedregal.";

    assertThat(service.identifyZone(text, zones)).isEmpty();
  }

  @Test
  void partOfAWordIsNotAMention() {
    Zone lima = zone("Lima");

    assertThat(service.identifyZone("Llegaron limas y víveres", List.of(lima))).isEmpty();
  }

  @Test
  void emergencyWithoutZonesIdentifiesNothing() {
    assertThat(service.identifyZone("Derrumbe en Alto Perú", List.of())).isEmpty();
  }

  private Zone zone(String name) {
    return emergency.registerZone(
        name, null, new PopulationImpact(null, 0, 0, 0, 0, 0), DamageLevel.MODERATE);
  }
}
