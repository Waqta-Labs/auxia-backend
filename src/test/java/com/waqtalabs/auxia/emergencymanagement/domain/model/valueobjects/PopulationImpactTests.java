package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.InconsistentPopulationImpactException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PopulationImpactTests {

  @Test
  void vulnerablePopulationAddsChildrenElderlyAndDisabledPeople() {
    var impact = new PopulationImpact(600, 35, 140, 40, 15, 6);

    assertThat(impact.vulnerablePopulation()).isEqualTo(61);
    assertThat(impact.knownPopulation()).contains(600);
  }

  @Test
  void populationIsOptional() {
    var impact = new PopulationImpact(null, 0, 140, 0, 0, 0);

    assertThat(impact.knownPopulation()).isEmpty();
  }

  @Test
  void aPersonCanBelongToSeveralVulnerableGroups() {
    var impact = new PopulationImpact(null, 1, 3, 3, 0, 3);

    assertThat(impact.vulnerablePopulation()).isEqualTo(6);
  }

  @ParameterizedTest
  @CsvSource({
    "141, 0, 0, children, peopleAffected",
    "0, 141, 0, elderlyPeople, peopleAffected",
    "0, 0, 141, disabledPeople, peopleAffected"
  })
  void vulnerableGroupCannotExceedPeopleAffected(
      int children, int elderly, int disabled, String count, String limit) {
    assertThatThrownBy(() -> new PopulationImpact(600, 35, 140, children, elderly, disabled))
        .isInstanceOfSatisfying(
            InconsistentPopulationImpactException.class,
            exception -> assertThat(exception.arguments()).containsExactly(count, limit));
  }

  @Test
  void peopleAffectedCannotExceedPopulation() {
    assertThatThrownBy(() -> new PopulationImpact(100, 35, 140, 0, 0, 0))
        .isInstanceOfSatisfying(
            InconsistentPopulationImpactException.class,
            exception ->
                assertThat(exception.arguments()).containsExactly("peopleAffected", "population"));
  }

  @Test
  void negativeCountIsRejected() {
    assertThatThrownBy(() -> new PopulationImpact(null, -1, 0, 0, 0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("familiesAffected");
  }

  @Test
  void populationMustBePositive() {
    assertThatThrownBy(() -> new PopulationImpact(0, 0, 0, 0, 0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("population");
  }
}
