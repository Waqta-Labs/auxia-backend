package com.waqtalabs.auxia.citizentransparency.domain.exceptions;

import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

/** The emergency is not registered: there is no public information about it. */
public class PublicEmergencyNotFoundException extends NotFoundException {

  public PublicEmergencyNotFoundException(EmergencyId emergencyId) {
    super("No public information about emergency " + emergencyId.value(), emergencyId.value());
  }
}
