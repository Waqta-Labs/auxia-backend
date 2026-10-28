package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** A recommendation needs an inventory with some available stock. */
public class NoAvailableStockException extends RuleViolationException {

  public NoAvailableStockException(EmergencyId emergencyId) {
    super(
        "There is no available stock to distribute in emergency "
            + emergencyId.value(),
        emergencyId.value());
  }
}
