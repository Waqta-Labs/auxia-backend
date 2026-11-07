package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GeolocationTests {

  @ParameterizedTest
  @CsvSource({"-90, -180", "90, 180", "-11.9436, -76.7078"})
  void acceptsCoordinatesWithinRange(double latitude, double longitude) {
    assertThatCode(() -> new Geolocation(latitude, longitude)).doesNotThrowAnyException();
  }

  @Test
  void distanceIsTheGreatCircleDistanceInKilometers() {
    var chosica = new Geolocation(-11.9436, -76.7078);
    var limaCenter = new Geolocation(-12.0464, -77.0428);

    assertThat(chosica.kilometersTo(limaCenter)).isCloseTo(38.2, within(0.5));
    assertThat(limaCenter.kilometersTo(chosica)).isEqualTo(chosica.kilometersTo(limaCenter));
    assertThat(chosica.kilometersTo(chosica)).isZero();
  }

  @ParameterizedTest
  @CsvSource({
    "-90.1, 0, latitude",
    "90.1, 0, latitude",
    "0, -180.1, longitude",
    "0, 181, longitude"
  })
  void rejectsCoordinatesOutOfRange(double latitude, double longitude, String field) {
    assertThatThrownBy(() -> new Geolocation(latitude, longitude))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(field);
  }
}
