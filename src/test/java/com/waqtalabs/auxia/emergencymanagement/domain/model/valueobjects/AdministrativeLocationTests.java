package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AdministrativeLocationTests {

  @Test
  void fullNameGoesFromTheDistrictToTheDepartment() {
    var location = new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica");

    assertThat(location.fullName()).isEqualTo("Lurigancho-Chosica, Lima, Lima");
  }

  @Test
  void valuesAreTrimmed() {
    var location = new AdministrativeLocation(" Piura ", "Piura ", " Castilla");

    assertThat(location).isEqualTo(new AdministrativeLocation("Piura", "Piura", "Castilla"));
  }

  @Test
  void blankValueIsRejected() {
    assertThatThrownBy(() -> new AdministrativeLocation("Lima", "", "Chosica"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("province");
  }
}
