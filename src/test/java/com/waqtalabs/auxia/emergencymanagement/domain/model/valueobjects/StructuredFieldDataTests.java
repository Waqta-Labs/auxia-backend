package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StructuredFieldDataTests {

  @Test
  void needsKeepTheOrderOfTheEnumeration() {
    var data = structured(Set.of(Need.MEDICINE, Need.WATER, Need.SHELTER), List.of());

    assertThat(data.needs()).containsExactly(Need.WATER, Need.SHELTER, Need.MEDICINE);
  }

  @Test
  void missingNeedsAndVariablesAreEmpty() {
    var data = structured(null, null);

    assertThat(data.needs()).isEmpty();
    assertThat(data.missingVariables()).isEmpty();
    assertThat(data.hasMissingVariables()).isFalse();
  }

  @Test
  void missingVariablesAreCopied() {
    List<String> missing = new ArrayList<>(List.of("disabledPeople"));
    var data = structured(Set.of(), missing);
    missing.add("children");

    assertThat(data.missingVariables()).containsExactly("disabledPeople");
    assertThat(data.hasMissingVariables()).isTrue();
  }

  @Test
  void negativeCountIsRejected() {
    assertThatThrownBy(
            () -> new StructuredFieldData(null, -1, null, null, null, null, Set.of(), List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static StructuredFieldData structured(Set<Need> needs, List<String> missing) {
    return new StructuredFieldData(DamageLevel.SEVERE, 35, 140, 40, 15, null, needs, missing);
  }
}
