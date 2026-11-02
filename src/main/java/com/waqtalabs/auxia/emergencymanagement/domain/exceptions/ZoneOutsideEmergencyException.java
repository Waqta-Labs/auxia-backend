package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** A field report can only be assigned to a zone of its own emergency. */
public class ZoneOutsideEmergencyException extends RuleViolationException {

  public ZoneOutsideEmergencyException(ZoneId zoneId, EmergencyId emergencyId) {
    super(
        "Zone " + zoneId.value() + " does not belong to emergency " + emergencyId.value(),
        zoneId.value(),
        emergencyId.value());
  }
}
