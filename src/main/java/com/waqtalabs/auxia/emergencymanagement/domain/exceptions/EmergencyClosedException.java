package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** A closed emergency does not accept new zones. */
public class EmergencyClosedException extends ConflictException {

  public EmergencyClosedException(EmergencyId id) {
    super("Emergency " + id.value() + " is closed and does not accept new zones", id.value());
  }
}
