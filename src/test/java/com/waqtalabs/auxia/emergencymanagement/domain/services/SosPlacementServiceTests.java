package com.waqtalabs.auxia.emergencymanagement.domain.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.services.SosPlacementService.Placement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SosPlacementServiceTests {

  private final SosPlacementService service = new SosPlacementService();

  private final Emergency chosica = emergency("Huaico Chosica 2026");
  private final Emergency piura = emergency("Inundación Piura 2026");
  private final Zone altoPeru = zone(chosica, "Alto Perú", new Geolocation(-11.9436, -76.7078));
  private final Zone pedregal = zone(chosica, "Pedregal", new Geolocation(-11.9300, -76.6900));
  private final Zone castilla = zone(piura, "Castilla", new Geolocation(-5.1945, -80.6328));
  private final Zone withoutLocation = zone(chosica, "Sin coordenadas", null);
  private final List<Emergency> active = List.of(chosica, piura);
  private final List<Zone> zones = List.of(altoPeru, pedregal, castilla, withoutLocation);

  @Test
  void sosGoesToTheNearestZoneWithinItsRadius() {
    var nearPedregal = new Geolocation(-11.9310, -76.6910);

    assertThat(service.place(Optional.of(nearPedregal), active, zones))
        .contains(new Placement(chosica.id(), Optional.of(pedregal)));
  }

  @Test
  void sosBeyondTheRadiusOfEveryZoneGoesToTheEmergencyOfTheNearestOnePendingZone() {
    var thirtyKilometersAway = new Geolocation(-11.6736, -76.7078);

    assertThat(service.place(Optional.of(thirtyKilometersAway), active, zones))
        .contains(new Placement(chosica.id(), Optional.empty()));
  }

  @Test
  void sosFarFromEveryZoneOrWithoutLocationIsNotPlacedAmongSeveralEmergencies() {
    var cusco = new Geolocation(-13.5320, -71.9675);

    assertThat(service.place(Optional.of(cusco), active, zones)).isEmpty();
    assertThat(service.place(Optional.empty(), active, zones)).isEmpty();
  }

  @Test
  void sosFarFromEveryZoneOrWithoutLocationGoesToTheOnlyActiveEmergency() {
    var cusco = new Geolocation(-13.5320, -71.9675);
    var onlyChosica = List.of(chosica);

    assertThat(service.place(Optional.of(cusco), onlyChosica, zones))
        .contains(new Placement(chosica.id(), Optional.empty()));
    assertThat(service.place(Optional.empty(), onlyChosica, List.of()))
        .contains(new Placement(chosica.id(), Optional.empty()));
  }

  @Test
  void zonesOfClosedEmergenciesAreIgnored() {
    piura.close();
    var inCastilla = new Geolocation(-5.1945, -80.6328);

    assertThat(service.place(Optional.of(inCastilla), active, zones))
        .contains(new Placement(chosica.id(), Optional.empty()));
  }

  @Test
  void withoutActiveEmergenciesNothingIsPlaced() {
    assertThat(service.place(Optional.of(new Geolocation(-11.9436, -76.7078)), List.of(), zones))
        .isEmpty();
  }

  private static Emergency emergency(String name) {
    return Emergency.register(
        name,
        EmergencyType.LANDSLIDE,
        null,
        new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica"),
        LocalDate.of(2026, 3, 14));
  }

  private static Zone zone(Emergency emergency, String name, Geolocation location) {
    return emergency.registerZone(
        name, location, new PopulationImpact(null, 0, 0, 0, 0, 0), DamageLevel.SEVERE);
  }
}
