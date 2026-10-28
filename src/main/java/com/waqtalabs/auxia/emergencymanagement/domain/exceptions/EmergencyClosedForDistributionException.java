package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** A closed emergency gets no new distribution plans. */
public class EmergencyClosedForDistributionException extends ConflictException {

  public EmergencyClosedForDistributionException(EmergencyId emergencyId) {
    super(
        "Emergency " + emergencyId.value()
            + " is closed and gets no new distribution plans",
        emergencyId.value());
  }
}
