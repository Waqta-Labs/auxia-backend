package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Indicators that the NLP extracted from a field report (US-02).
 *
 * <p>An indicator that could not be identified is {@code null}, and its name is listed in {@code
 * missingVariables} so the report is marked for review (US-02, scenario 2).
 *
 * @param missingVariables names of the indicators that could not be identified, such as {@code
 *     disabledPeople}
 */
public record StructuredFieldData(
    DamageLevel damageLevel,
    Integer familiesAffected,
    Integer peopleAffected,
    Integer children,
    Integer elderlyPeople,
    Integer disabledPeople,
    Set<Need> needs,
    List<String> missingVariables) {

  public StructuredFieldData {
    requireNotNegative(familiesAffected, "familiesAffected");
    requireNotNegative(peopleAffected, "peopleAffected");
    requireNotNegative(children, "children");
    requireNotNegative(elderlyPeople, "elderlyPeople");
    requireNotNegative(disabledPeople, "disabledPeople");
    needs = inDeclarationOrder(needs);
    missingVariables = missingVariables == null ? List.of() : List.copyOf(missingVariables);
  }

  public boolean hasMissingVariables() {
    return !missingVariables.isEmpty();
  }

  private static Set<Need> inDeclarationOrder(Collection<Need> needs) {
    if (needs == null || needs.isEmpty()) {
      return Set.of();
    }
    return Collections.unmodifiableSet(EnumSet.copyOf(needs));
  }

  private static void requireNotNegative(Integer value, String name) {
    if (value != null && value < 0) {
      throw new IllegalArgumentException(name + " must not be negative");
    }
  }
}
