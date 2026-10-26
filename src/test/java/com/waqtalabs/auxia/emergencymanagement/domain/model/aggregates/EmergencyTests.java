package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyAlreadyClosedException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyStatus;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EmergencyTests {

  private static final AdministrativeLocation CHOSICA =
      new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica");

  @Test
  void registeredEmergencyIsActive() {
    Emergency emergency = register();

    assertThat(emergency.id()).isNotNull();
    assertThat(emergency.status()).isEqualTo(EmergencyStatus.ACTIVE);
    assertThat(emergency.isActive()).isTrue();
    assertThat(emergency.name()).isEqualTo("Huaico Chosica 2026");
    assertThat(emergency.type()).isEqualTo(EmergencyType.LANDSLIDE);
    assertThat(emergency.location()).isEqualTo(CHOSICA);
    assertThat(emergency.startDate()).isEqualTo(LocalDate.of(2026, 3, 14));
  }

  @Test
  void registeredEmergenciesGetDifferentIdentifiers() {
    assertThat(register().id()).isNotEqualTo(register().id());
  }

  @Test
  void blankDescriptionIsStoredAsAbsent() {
    Emergency emergency =
        Emergency.register(
            "Huaico Chosica 2026", EmergencyType.LANDSLIDE, "  ", CHOSICA, LocalDate.now());

    assertThat(emergency.description()).isEmpty();
  }

  @Test
  void blankNameIsRejected() {
    assertThatThrownBy(
            () -> Emergency.register(" ", EmergencyType.FLOOD, null, CHOSICA, LocalDate.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void closingAnActiveEmergencyClosesIt() {
    Emergency emergency = register();

    emergency.close();

    assertThat(emergency.status()).isEqualTo(EmergencyStatus.CLOSED);
    assertThat(emergency.isActive()).isFalse();
  }

  @Test
  void closingAClosedEmergencyIsRejected() {
    Emergency emergency = register();
    emergency.close();

    assertThatThrownBy(emergency::close)
        .isInstanceOfSatisfying(
            EmergencyAlreadyClosedException.class,
            exception ->
                assertThat(exception.arguments()).containsExactly(emergency.id().value()));
  }

  @Test
  void reconstitutedEmergencyKeepsItsStatus() {
    Emergency stored = register();

    Emergency emergency =
        Emergency.reconstitute(
            stored.id(),
            stored.name(),
            stored.type(),
            "Lluvias intensas",
            stored.location(),
            stored.startDate(),
            EmergencyStatus.CLOSED);

    assertThat(emergency.id()).isEqualTo(stored.id());
    assertThat(emergency.isActive()).isFalse();
    assertThat(emergency.description()).contains("Lluvias intensas");
  }

  private static Emergency register() {
    return Emergency.register(
        "Huaico Chosica 2026",
        EmergencyType.LANDSLIDE,
        "Huaico tras lluvias intensas",
        CHOSICA,
        LocalDate.of(2026, 3, 14));
  }
}
