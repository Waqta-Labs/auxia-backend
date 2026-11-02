package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

public class EmergencyNotFoundException extends NotFoundException {

  public EmergencyNotFoundException(EmergencyId id) {
    super("Emergency " + id.value() + " does not exist", id.value());
  }
}
