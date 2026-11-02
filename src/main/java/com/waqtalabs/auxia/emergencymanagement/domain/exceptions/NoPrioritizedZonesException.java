package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** A recommendation needs at least one zone with an urgency score and identified needs. */
public class NoPrioritizedZonesException extends RuleViolationException {

  public NoPrioritizedZonesException(EmergencyId emergencyId) {
    super(
        "Emergency " + emergencyId.value()
            + " has no zone with a score and identified needs",
        emergencyId.value());
  }
}
