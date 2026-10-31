package com.waqtalabs.auxia.citizentransparency.domain.exceptions;

import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

/** The zone is not registered yet: there is no public information about it (US-24). */
public class PublicZoneNotFoundException extends NotFoundException {

  public PublicZoneNotFoundException(ZoneId zoneId) {
    super("No public information about zone " + zoneId.value(), zoneId.value());
  }
}
