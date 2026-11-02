package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** A new recommendation waits until the proposed plans of the emergency are decided, so the stock
 * they hold always belongs to a visible plan. */
public class PendingDistributionPlansException extends ConflictException {

  public PendingDistributionPlansException(EmergencyId emergencyId) {
    super(
        "Emergency " + emergencyId.value()
            + " has proposed distribution plans to decide first",
        emergencyId.value());
  }
}
