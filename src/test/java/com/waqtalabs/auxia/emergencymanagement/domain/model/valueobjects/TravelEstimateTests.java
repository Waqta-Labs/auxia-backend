package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class TravelEstimateTests {

  @Test
  void travelGoesInAStraightLineAtThirtyKilometersPerHourRoundedUp() {
    var base = new Geolocation(-11.9436, -76.6528);
    var zone = new Geolocation(-11.9436, -76.7078);

    TravelEstimate estimate = TravelEstimate.between(base, zone);

    assertThat(estimate.kilometers()).isCloseTo(5.99, within(0.01));
    assertThat(estimate.minutes()).isEqualTo(12);
  }

  @Test
  void samePlaceTakesNoTime() {
    var zone = new Geolocation(-11.9436, -76.7078);

    assertThat(TravelEstimate.between(zone, zone)).isEqualTo(new TravelEstimate(0, 0));
  }
}
