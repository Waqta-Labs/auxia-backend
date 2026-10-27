package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

public class EmergencyAlreadyClosedException extends ConflictException {

  public EmergencyAlreadyClosedException(EmergencyId id) {
    super("Emergency " + id.value() + " is already closed", id.value());
  }
}
