package com.waqtalabs.auxia.emergencymanagement.infrastructure.integration.aiservice;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyIndicators;
import java.util.List;

/**
 * {@code indicators} object of the AI Service contract: the output of the structuring endpoint
 * and the input of the urgency score. The enumerations share their values with the domain.
 */
record AiServiceIndicators(
    DamageLevel damageLevel,
    Integer familiesAffected,
    Integer peopleAffected,
    Integer children,
    Integer elderlyPeople,
    Integer disabledPeople,
    List<Need> needs) {

  static AiServiceIndicators from(UrgencyIndicators indicators) {
    return new AiServiceIndicators(
        indicators.damageLevel(),
        indicators.familiesAffected(),
        indicators.peopleAffected(),
        indicators.children(),
        indicators.elderlyPeople(),
        indicators.disabledPeople(),
        List.copyOf(indicators.needs()));
  }
}
